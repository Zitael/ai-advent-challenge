param(
    [switch]$InstallLargeModels
)

$ErrorActionPreference = "Stop"

function Invoke-Ollama {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    Write-Host ""
    Write-Host "ollama $($Arguments -join ' ')"

    & ollama @Arguments

    if ($LASTEXITCODE -ne 0) {
        throw "Ollama command failed with exit code $LASTEXITCODE"
    }
}

Write-Host "Checking Ollama installation..."

Invoke-Ollama -Arguments @("--version")

Write-Host ""
Write-Host "Installing autocomplete model..."

Invoke-Ollama -Arguments @(
    "pull",
    "qwen2.5-coder:1.5b-base"
)

Write-Host ""
Write-Host "Installing default chat model..."

Invoke-Ollama -Arguments @(
    "pull",
    "qwen2.5-coder:7b"
)

Invoke-Ollama -Arguments @(
    "create",
    "ai-advent-qwen-coder:7b",
    "-f",
    "local-llm/Modelfile.qwen2.5-coder-7b"
)

if ($InstallLargeModels) {
    Write-Host ""
    Write-Host "Installing additional benchmark models..."

    Invoke-Ollama -Arguments @(
        "pull",
        "qwen2.5-coder:14b"
    )

    Invoke-Ollama -Arguments @(
        "create",
        "ai-advent-qwen-coder:14b",
        "-f",
        "local-llm/Modelfile.qwen2.5-coder-14b"
    )

    Invoke-Ollama -Arguments @(
        "pull",
        "deepseek-coder-v2:16b"
    )

    Invoke-Ollama -Arguments @(
        "create",
        "ai-advent-deepseek-coder-v2",
        "-f",
        "local-llm/Modelfile.deepseek-coder-v2"
    )
}

Write-Host ""
Write-Host "Installed Ollama models:"

Invoke-Ollama -Arguments @("list")

Write-Host ""
Write-Host "Local model setup completed."