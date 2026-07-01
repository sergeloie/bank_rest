import { tool } from "@mimo-ai/plugin"
import { execSync } from "child_process"

export default tool({
  description: "Call an Amplicode MCP tool. Use for project analysis, build info, Spring beans, endpoints, entities, debugging. The MCP server runs at http://127.0.0.1:64442/sse.",
  args: {
    tool: tool.schema.string().describe("MCP tool name, e.g. get_project_summary, list_spring_bean, list_project_endpoints"),
    arguments: tool.schema.object({}).optional().describe("Arguments to pass to the MCP tool"),
  },
  async execute(args, ctx) {
    const argsJson = args.arguments ? JSON.stringify(args.arguments) : "{}"
    const toolName = args.tool

    const psScript = `
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

$toolName = '${toolName.replace(/'/g, "''")}'
$argsJson = '${argsJson.replace(/'/g, "''")}'

$client = [System.Net.Http.HttpClient]::new()
$cts = [System.Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds(20))
$req = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, 'http://127.0.0.1:64442/sse')
$resp = $client.SendAsync($req, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead, $cts.Token).GetAwaiter().GetResult()
$stream = $resp.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
$reader = [System.IO.StreamReader]::new($stream)

$buf = New-Object char[] 4096
$n = $reader.Read($buf, 0, 4096)
$raw = [string]::new($buf, 0, $n)
$sid = [regex]::Match($raw, 'sessionId=([a-f0-9-]+)').Groups[1].Value

$jsonBody = @{ jsonrpc='2.0'; id=1; method='tools/call'; params=@{ name=$toolName; arguments=($argsJson | ConvertFrom-Json) } } | ConvertTo-Json -Depth 10 -Compress

$pc = [System.Net.Http.HttpClient]::new()
$pr = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, "http://127.0.0.1:64442/message?sessionId=$sid")
$pr.Content = [System.Net.Http.StringContent]::new($jsonBody, [System.Text.Encoding]::UTF8, 'application/json')
$cts2 = [System.Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds(30))
$pc.SendAsync($pr, $cts2.Token).GetAwaiter().GetResult() | Out-Null

Start-Sleep -Seconds 5

$buf2 = New-Object char[] 65536
$n2 = $reader.Read($buf2, 0, 65536)
$responseRaw = [string]::new($buf2, 0, $n2)

$dataLine = ($responseRaw -split "\\n" | Where-Object { $_ -match '^data: ' }) -replace '^data: ',''

$client.Dispose()
$pc.Dispose()

Write-Output $dataLine
`

    try {
      const result = execSync(
        `powershell -NoProfile -NonInteractive -Command "${psScript.replace(/"/g, '`"')}"`,
        { encoding: "utf-8", cwd: ctx.directory, timeout: 60000 }
      )

      let parsed
      try {
        const lines = result.trim().split("\n")
        const lastLine = lines[lines.length - 1].trim()
        parsed = JSON.parse(lastLine)
      } catch {
        return result
      }

      if (parsed.result?.content) {
        const texts = parsed.result.content
          .filter((c: any) => c.type === "text")
          .map((c: any) => c.text)
        const combined = texts.join("\n")

        if (parsed.result.structuredContent) {
          return JSON.stringify(parsed.result.structuredContent, null, 2)
        }
        return combined
      }

      if (parsed.error) {
        return `MCP Error: ${JSON.stringify(parsed.error)}`
      }

      return JSON.stringify(parsed, null, 2)
    } catch (e: any) {
      return `MCP call failed: ${e.message}`
    }
  },
})
