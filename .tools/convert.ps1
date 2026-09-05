$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Web.Extensions

$src = 'D:\FitLog\.exercises.json'
$outJson = 'D:\FitLog\app\src\main\assets\dataset\exercises.json'
$outImgs = 'D:\FitLog\.tools\image-list.txt'
New-Item -ItemType Directory -Force -Path (Split-Path $outJson) | Out-Null

$text = [System.IO.File]::ReadAllText($src, [System.Text.Encoding]::UTF8)
$ser = New-Object System.Web.Script.Serialization.JavaScriptSerializer
$ser.MaxJsonLength = [int]::MaxValue
$ser.RecursionLimit = 64
$list = $ser.DeserializeObject($text)

$partMap = @{
  'chest' = '胸'; 'back' = '背'; 'shoulders' = '肩'
  'upper arms' = '手臂'; 'lower arms' = '手臂'
  'upper legs' = '腿'; 'lower legs' = '腿'
  'waist' = '腹部'; 'cardio' = '有氧'; 'neck' = '肩'
}
$equipMap = @{
  'dumbbell' = '哑铃'
  'barbell' = '杠铃'; 'olympic barbell' = '杠铃'; 'ez barbell' = '杠铃'; 'trap bar' = '杠铃'
  'cable' = '固定器械'; 'rope' = '固定器械'; 'leverage machine' = '固定器械'; 'smith machine' = '固定器械'
  'sled machine' = '固定器械'; 'assisted' = '固定器械'
  'stationary bike' = '固定器械'; 'upper body ergometer' = '固定器械'; 'skierg machine' = '固定器械'
  'elliptical machine' = '固定器械'; 'stepmill machine' = '固定器械'
  'band' = '弹力带'; 'resistance band' = '弹力带'
  'kettlebell' = '壶铃'; 'body weight' = '徒手'
  'weighted' = '其他'; 'stability ball' = '其他'; 'bosu ball' = '其他'; 'medicine ball' = '其他'
  'roller' = '其他'; 'wheel roller' = '其他'; 'hammer' = '其他'; 'tire' = '其他'
}

# 动作名翻译词典：长词组优先替换
$d = [ordered]@{
  'chest press' = '推胸'; 'chest dip' = '胸臂屈伸'; 'pec deck' = '蝴蝶机'; 'chest' = '胸'; 'stretch' = '拉伸'; 'kneeling' = '跪姿'; 'archer' = '射手式'; 'wide-grip' = '宽距'; 'narrow-grip' = '窄距'; 'reverse grip' = '反握'; 'underhand grip' = '反握'; 'overhand grip' = '正握'; 'close-grip bench press' = '窄距卧推'; 'wide-grip bench press' = '宽距卧推'
  'bench press' = '卧推'; 'incline bench' = '上斜凳'; 'bench' = '凳'
  'front squat' = '前蹲'; 'overhead squat' = '过头深蹲'; 'goblet squat' = '高脚杯深蹲'
  'split squat' = '分腿蹲'; 'jump squat' = '跳蹲'; 'squat' = '深蹲'
  'romanian deadlift' = '罗马尼亚硬拉'; 'stiff leg deadlift' = '直腿硬拉'
  'single leg deadlift' = '单腿硬拉'; 'sumo deadlift' = '相扑硬拉'; 'deadlift' = '硬拉'
  'hip thrust' = '臀推'; 'glute bridge' = '臀桥'; 'good morning' = '早安式体前屈'
  'leg press' = '腿举'; 'leg extension' = '腿屈伸'; 'leg curl' = '腿弯举'
  'standing calf raise' = '站姿提踵'; 'seated calf raise' = '坐姿提踵'; 'calf raise' = '提踵'
  'step-up' = '台阶步'; 'step up' = '台阶步'; 'lunge' = '弓步'; 'side kickback' = '侧后踢'
  'clean and press' = '高翻推举'; 'clean' = '高翻'; 'snatch' = '抓举'; 'swing' = '摆荡'
  'shoulder press' = '肩推'; 'overhead press' = '过头推举'; 'military press' = '军式推举'
  'arnold press' = '阿诺德推举'; 'lateral raise' = '侧平举'; 'front raise' = '前平举'
  'rear delt' = '三角肌后束'; 'upright row' = '直立划船'; 'shrug' = '耸肩'
  'face pull' = '面拉'; 'pull-up' = '引体向上'; 'pull up' = '引体向上'
  'chin-up' = '反手引体向上'; 'chin up' = '反手引体向上'; 'pulldown' = '下拉'
  'lat pull-down' = '高位下拉'; 'push-up' = '俯卧撑'; 'push up' = '俯卧撑'
  'skull crusher' = '仰卧臂屈伸'; 'close-grip' = '窄距'; 'kickback' = '臂屈伸'
  'bicep curl' = '二头弯举'; 'hammer curl' = '锤式弯举'; 'concentration curl' = '集中弯举'
  'preacher curl' = '牧师凳弯举'; 'wrist curl' = '腕弯举'; 'curl' = '弯举'
  'triceps extension' = '三头臂屈伸'; 'triceps dip' = '三头臂屈伸'; 'extension' = '屈伸'
  'bent over row' = '俯身划船'; 'single-arm row' = '单臂划船'; 'chest supported row' = '斜托划船'
  'row' = '划船'; 'pullover' = '仰卧上拉'; 'crossover' = '龙门架夹胸'; 'flye' = '飞鸟'
  'fly' = '飞鸟'; 'crunch' = '卷腹'; 'sit-up' = '仰卧起坐'; 'sit up' = '仰卧起坐'
  'leg raise' = '举腿'; 'knee raise' = '提膝'; 'plank' = '平板支撑'
  'russian twist' = '俄罗斯转体'; 'mountain climber' = '登山跑'; 'burpee' = '波比跳'
  'jumping jack' = '开合跳'; 'high knees' = '高抬腿'; 'butt kicks' = '后踢腿跑'
  'jump rope' = '跳绳'; 'superman' = '超人式'; 'back extension' = '背屈伸'
  'ab wheel' = '健腹轮'; 'dead bug' = '死虫式'; 'bird dog' = '鸟狗式'
  'v-up' = 'V字两头起'; 'flutter kick' = '打腿'; 'scissor kick' = '剪刀腿'
  'dip' = '臂屈伸'; 'press' = '推举'; 'raise' = '平举'
  'barbell' = '杠铃'; 'dumbbell' = '哑铃'; 'kettlebell' = '壶铃'
  'smith machine' = '史密斯机'; 'cable' = '绳索'; 'rope' = '绳索'
  'band' = '弹力带'; 'resistance band' = '弹力带'; 'lever' = '器械'
  'ez barbell' = '曲杆'; 'olympic barbell' = '奥杆'; 'trap bar' = '六角杠'
  'medicine ball' = '药球'; 'stability ball' = '瑞士球'; 'bosu ball' = 'bosu球'
  'weighted' = '负重'; 'assisted' = '辅助'; 'sled' = '雪橇机'; 'suspended' = '悬挂'
  'incline' = '上斜'; 'decline' = '下斜'; 'seated' = '坐姿'; 'standing' = '站姿'
  'lying' = '仰卧'; 'bent over' = '俯身'; 'single arm' = '单臂'; 'single leg' = '单腿'
  'one arm' = '单臂'; 'one leg' = '单腿'; 'alternate' = '交替'; 'alternating' = '交替'
  'wide' = '宽距'; 'narrow' = '窄距'; 'reverse' = '反向'; 'behind neck' = '颈后'
  'cross body' = '交叉'; 'isometric' = '等长'; 'jump' = '跳跃'; 'side' = '侧'
  'neck' = '颈部'
}
$keys = @($d.Keys | Sort-Object { $_.Length } -Descending)

$muscleMap = @{
  'pectoralis' = '胸大肌'; 'biceps' = '肱二头肌'; 'triceps' = '肱三头肌'; 'brachialis' = '肱肌'
  'abs' = '腹肌'; 'abdominals' = '腹肌'; 'obliques' = '腹斜肌'; 'quadriceps' = '股四头肌'
  'hamstrings' = '腘绳肌'; 'glutes' = '臀大肌'; 'gluteus' = '臀大肌'; 'lats' = '背阔肌'
  'traps' = '斜方肌'; 'deltoids' = '三角肌'; 'delt' = '三角肌'; 'calves' = '小腿'
  'forearms' = '前臂'; 'forearm' = '前臂'; 'upper back' = '上背'; 'lower back' = '下背'
  'adductors' = '内收肌'; 'abductors' = '外展肌'; 'serratus' = '前锯肌'
  'gastrocnemius' = '腓肠肌'; 'soleus' = '比目鱼肌'; 'rotator cuff' = '肩袖'
  'hip flexors' = '髋屈肌'; 'neck' = '颈部'; 'cardio' = '心肺'
}

function Translate([string]$s) {
  $n = $s.ToLower()
  foreach ($k in $keys) {
    if ($n.Contains($k)) { $n = $n.Replace($k, ' ' + $d[$k] + ' ') }
  }
  $n = [regex]::Replace($n, '(?<=[\u4e00-\u9fa5])\s+(?=[\u4e00-\u9fa5])', '')
  $n = [regex]::Replace($n, '\s+', ' ').Trim(' ', '-', '/')
  $n = $n.Replace(' -', '－').Replace('- ', '－')
  $n = $n -replace '\(\s*', '（'
  $n = $n -replace '\s*\)', '）'
  return $n
}
function TranslateMuscle([string]$s) {
  $n = $s.ToLower()
  foreach ($k in $muscleMap.Keys) {
    if ($n.Contains($k)) { return $muscleMap[$k] }
  }
  return $s
}

$out = New-Object System.Collections.Generic.List[object]
$imgList = New-Object System.Collections.Generic.List[string]
$zhNameCount = 0

foreach ($e in $list) {
  $id = [string]$e['id']
  $bp = [string]$e['body_part']
  $eq = [string]$e['equipment']
  $tg = [string]$e['target']

  $part = if ($partMap.ContainsKey($bp)) { $partMap[$bp] } else { '其他' }
  # 腿部动作目标为臀大肌 → 归臀部
  if ($bp -eq 'upper legs' -and $tg -match 'glut') { $part = '臀部' }
  $equip = if ($equipMap.ContainsKey($eq)) { $equipMap[$eq] } else { '其他' }
  $mode = 'wr'
  if ($bp -eq 'cardio') { $mode = 't' } elseif ($eq -eq 'body weight') { $mode = 'r' }

  $name = Translate([string]$e['name'])
  if ($name -match '[\u4e00-\u9fa5]') { $zhNameCount++ }

  $insZh = $e['instructions']['zh']
  $insEn = $e['instructions']['en']
  $tipSrc = if ($insZh -and $insZh.Trim().Length -gt 0) { $insZh } else { $insEn }
  $tip = ($tipSrc -replace '\s+', ' ').Trim()
  if ($tip.Length -gt 160) { $tip = $tip.Substring(0, 160) + '…' }

  $stepsZh = $e['instruction_steps']['zh']
  $steps = if ($stepsZh -and $stepsZh.Count -gt 0) { ($stepsZh -join "`n") } else { $tip }

  $tgZh = TranslateMuscle($tg)

  $out.Add(@{
      id = $id; n = $name; p = $part; e = $equip; m = $mode; t = $tip
      s = $steps; tg = $tgZh; mus = [string]$e['muscle_group']
    })
  $imgList.Add("$id|$([string]$e['image'])")
}

$json = $ser.Serialize($out.ToArray())
$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($outJson, $json, $utf8)
[System.IO.File]::WriteAllLines($outImgs, $imgList, $utf8)

Write-Output ("imported=" + $out.Count + " zhNames=" + $zhNameCount)
Write-Output ("jsonKB=" + [math]::Round((Get-Item $outJson).Length / 1KB))
