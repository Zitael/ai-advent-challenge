param(
    [string[]]$Models = @(
        "ai-advent-qwen-coder:7b",
        "ai-advent-qwen-coder:14b",
        "ai-advent-deepseek-coder-v2"
    )
)

$ErrorActionPreference = "Stop"

$TasksDirectory = "local-llm/benchmark/tasks"
$ResultsDirectory = "local-llm/benchmark/results"

New-Item `
    -ItemType Directory `
    -Force `
    -Path $ResultsDirectory |
    Out-Null

$Tasks = Get-ChildItem `
    -Path "$TasksDirectory/*.md" |
    Sort-Object Name

if ($Tasks.Count -eq 0) {
    throw "No benchmark tasks found in $TasksDirectory"
}

foreach ($Model in $Models) {
    $SafeModelName = $Model -replace '[:/\\]', '-'

    foreach ($Task in $Tasks) {
        $Prompt = Get-Content `
            -Path $Task.FullName `
            -Raw

        $ResultFile = Join-Path `
            $ResultsDirectory `
            "$SafeModelName-$($Task.BaseName).md"

        $MetricsFile = Join-Path `
            $ResultsDirectory `
            "$SafeModelName-$($Task.BaseName).metrics.txt"

        Write-Host ""
        Write-Host "Running model: $Model"
        Write-Host "Task: $($Task.Name)"

        $Stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

        $Prompt |
            & ollama run $Model |
            Tee-Object -FilePath $ResultFile

        $ExitCode = $LASTEXITCODE

        $Stopwatch.Stop()

        @"
model=$Model
task=$($Task.Name)
exit_code=$ExitCode
elapsed_seconds=$([math]::Round($Stopwatch.Elapsed.TotalSeconds, 2))
finished_at=$((Get-Date).ToString("o"))
"@ |
            Set-Content `
                -Path $MetricsFile `
                -Encoding UTF8

        if ($ExitCode -ne 0) {
            Write-Warning (
                "Benchmark failed for model '$Model', " +
                "task '$($Task.Name)', exit code $ExitCode"
            )
        }
    }
}

Write-Host ""
Write-Host "Benchmark completed."
Write-Host "Results: $ResultsDirectory"