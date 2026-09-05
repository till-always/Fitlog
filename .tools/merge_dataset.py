# 从上游 exercises-dataset 全量数据重建项目 exercises.json：
# - 保留现有 JSON 的中文名(n)/教学(t/s)/记录方式(m)/目标肌群(tg)
# - 用上游 body_part/target 重新推导更细的部位 p（11 类：拆出 前臂/小腿，新增 颈）
# - 用上游 equipment 重新映射更细的器械 e（12 类：健身球/药球/负重/战绳/滚轮/有氧器械）
# - 合并 en(英文名)/sec(次要肌群)；修复名字里的 "в°" 乱码
# - GIF 路径由导入逻辑按 id 推导（exg/{id}.gif），不入 JSON
import json
from collections import Counter

NEW = r'C:\Users\till\AppData\Local\Temp\exercises-dataset\data\exercises.json'
CUR = r'D:\FitLog\app\src\main\assets\dataset\exercises.json'

with open(NEW, encoding='utf-8') as f:
    new = json.load(f)
with open(CUR, encoding='utf-8') as f:
    cur = json.load(f)

new_by_id = {r['id']: r for r in new}
assert len(new_by_id) == len(new) == len(cur)


def part_of(r):
    bp, tg = r['body_part'], r['target']
    if bp == 'chest': return '胸'
    if bp == 'back': return '背'
    if bp == 'shoulders': return '肩'
    if bp == 'upper arms': return '手臂'
    if bp == 'lower arms': return '前臂'
    if bp == 'waist': return '腹部'
    if bp == 'upper legs': return '臀部' if tg == 'glutes' else '腿'
    if bp == 'lower legs': return '小腿'
    if bp == 'cardio': return '有氧'
    if bp == 'neck': return '颈'
    return '其他'


EQ = {
    'barbell': '杠铃', 'ez barbell': '杠铃', 'olympic barbell': '杠铃', 'trap bar': '杠铃',
    'dumbbell': '哑铃', 'kettlebell': '壶铃', 'body weight': '徒手',
    'cable': '固定器械', 'leverage machine': '固定器械', 'smith machine': '固定器械',
    'sled machine': '固定器械', 'assisted': '固定器械',
    'band': '弹力带', 'resistance band': '弹力带',
    'stability ball': '健身球', 'bosu ball': '健身球',
    'medicine ball': '药球', 'weighted': '负重', 'hammer': '负重', 'tire': '负重',
    'rope': '战绳', 'roller': '滚轮', 'wheel roller': '滚轮',
    'upper body ergometer': '有氧器械', 'skierg machine': '有氧器械',
    'stationary bike': '有氧器械', 'elliptical machine': '有氧器械', 'stepmill machine': '有氧器械',
}

pc = Counter()
ec = Counter()
moji = 0
for r in cur:
    u = new_by_id[r['id']]
    r['p'] = part_of(u)
    r['e'] = EQ[u['equipment']]
    r['en'] = u['name']
    r['sec'] = u['secondary_muscles']
    if u['muscle_group'] != r.get('mus'):
        r['mus'] = u['muscle_group']
    if 'в°' in r['n']:
        r['n'] = r['n'].replace('в°', '°')
        moji += 1
    pc[r['p']] += 1
    ec[r['e']] += 1

# 数据集内重名（导入按名去重会跳过后出现的）
dup = [n for n, c in Counter(r['n'] for r in cur).items() if c > 1]

# 示例计划引用的动作必须存在于数据集
DEMO = ['杠铃卧推', '器械推胸', '哑铃侧平举', '绳索站姿 up straight 龙门架夹胸 s', '俯卧撑',
        '引体向上', '交替 lateral 下拉', '绳索坐姿划船', '杠铃弯举',
        '杠铃 full 深蹲', '雪橇机 45° 腿举', '杠铃臀桥', '登山跑']
names = {r['n'] for r in cur}
missing = [d for d in DEMO if d not in names]
assert not missing, f'demo names missing: {missing}'

with open(CUR, 'w', encoding='utf-8') as f:
    json.dump(cur, f, ensure_ascii=False, separators=(',', ':'))

print('parts:', dict(pc))
print('equips:', dict(ec))
print('mojibake fixed:', moji, '| duplicate names:', len(dup), dup)
import os
print('out size MB:', round(os.path.getsize(CUR) / 1048576, 2))
