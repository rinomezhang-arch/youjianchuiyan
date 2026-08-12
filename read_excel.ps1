$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false
$wb = $excel.Workbooks.Open("F:\又见炊烟私房菜宴会预定数据库\又见炊烟私房菜宴会预定数据库\宴会资料\宴会菜单模板.xlsx")
Write-Host "Sheets count: $($wb.Sheets.Count)"
foreach($ws in $wb.Sheets) {
    Write-Host "=== Sheet: $($ws.Name) ==="
    $usedRange = $ws.UsedRange
    $rows = $usedRange.Rows.Count
    $cols = $usedRange.Columns.Count
    Write-Host "Rows: $rows Cols: $cols"
    for($r=1; $r -le [Math]::Min($rows, 100); $r++) {
        $line = ""
        for($c=1; $c -le [Math]::Min($cols, 15); $c++) {
            $val = $ws.Cells.Item($r,$c).Text
            if($c -gt 1) { $line += "|" }
            $line += $val
        }
        Write-Host $line
    }
}
$wb.Close($false)
$excel.Quit()
[System.Runtime.InteropServices.Marshal]::ReleaseComObject($excel) | Out-Null
