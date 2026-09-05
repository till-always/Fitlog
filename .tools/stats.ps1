$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Web.Extensions
$src = 'D:\FitLog\.exercises.json'
$text = [System.IO.File]::ReadAllText($src, [System.Text.Encoding]::UTF8)
$ser = New-Object System.Web.Script.Serialization.JavaScriptSerializer
$ser.MaxJsonLength = [int]::MaxValue
$ser.RecursionLimit = 32
$list = $ser.DeserializeObject($text)
Write-Output ("count=" + $list.Count)

$bp = @{}; $eq = @{}
foreach ($e in $list) {
    $b = [string]$e['body_part']; $q = [string]$e['equipment']
    if ($bp.ContainsKey($b)) { $bp[$b]++ } else { $bp[$b] = 1 }
    if ($eq.ContainsKey($q)) { $eq[$q]++ } else { $eq[$q] = 1 }
}
Write-Output '--- body_part ---'
$bp.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object { Write-Output ("{0} = {1}" -f $_.Key, $_.Value) }
Write-Output '--- equipment ---'
$eq.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object { Write-Output ("{0} = {1}" -f $_.Key, $_.Value) }

$s0 = $list[0]
Write-Output ("sample name=" + $s0['name'] + " image=" + $s0['image'] + " gif=" + $s0['gif_url'])
$zh = $s0['instruction_steps']['zh']
Write-Output ("zh_steps=" + ($zh -join ' | ').Substring(0, 200))
