# 动作库人工精选：
# - 删除器械分类：滚轮/负重/药球/健身球（连同其中动作）
# - 战绳分类只保留 跳绳/战绳绳索 并划入 有氧器械，分类撤销
# - 弹力带/徒手按市面常见度精选（保留 ID 白名单）
# - 新增分类：龙门架绳索(cable)、史密斯机(smith machine)；leverage/sled/assisted 仍归 固定器械
# - 少量名字润色
# 运行顺序：merge_dataset.py → translate_names.py → 本脚本
import json
from collections import Counter

CUR = r'D:\FitLog\app\src\main\assets\dataset\exercises.json'
NEW = r'C:\Users\till\AppData\Local\Temp\exercises-dataset\data\exercises.json'
with open(CUR, encoding='utf-8') as f:
    data = json.load(f)
with open(NEW, encoding='utf-8') as f:
    new = json.load(f)
up_equip = {r['id']: r['equipment'] for r in new}

# 固定器械细分：cable → 龙门架绳索，smith machine → 史密斯机（leverage/sled/assisted 仍归固定器械）
for r in data:
    eq = up_equip.get(r['id'])
    if eq == 'cable':
        r['e'] = '龙门架绳索'
    elif eq == 'smith machine':
        r['e'] = '史密斯机'

DROP_EQUIP = {'滚轮', '负重', '药球', '健身球'}
ROPE_KEEP = {'0128', '2612'}          # 战绳绳索、跳绳 → 有氧器械
BAND_KEEP = {
    '0968', '0970', '0974', '0975', '0976', '0978', '0979', '0983', '0986', '0988',
    '0991', '0993', '0994', '0998', '0999', '1001', '1004', '1010', '1013', '1016',
    '1017', '1018', '1022', '1254', '1369', '3006', '3122', '3123', '3124', '3144',
}
BW_KEEP = {
    '0001', '0002', '0003', '0006', '0130', '0251', '0259', '0276', '0279', '0283',
    '0284', '0456', '0459', '0464', '0472', '0475', '0489', '0493', '0499', '0501',
    '0514', '0613', '0628', '0630', '0643', '0651', '0652', '0659', '0662', '0664',
    '0669', '0685', '0687', '0690', '0705', '0710', '0716', '0721', '0735', '0794',
    '0814', '0871', '0872', '1271', '1306', '1311', '1326', '1346', '1363', '1365',
    '1366', '1368', '1373', '1377', '1387', '1390', '1403', '1405', '1418', '1424',
    '1427', '1428', '1429', '1460', '1471', '1476', '1490', '1494', '1511', '1585',
    '1604', '1688', '1761', '2329', '2368', '2567', '3013', '3016', '3119', '3211',
    '3224', '3239', '3361', '3470', '3523', '3533', '3582', '3645', '3699',
}
RENAME = {
    '0991': '弹力带胯下拉', '0735': '仰卧起坐', '3224': '开合跳', '3211': '跪姿俯卧撑',
    '3470': '前弓步', '1373': '站姿提踵', '1387': '单腿提踵', '3013': '臀桥',
    '0705': '侧平板支撑', '3119': '全蹲保持', '3699': '平板支撑点肩', '0464': '平板支撑转体',
    '3239': '跪姿平板支撑点肩',
}

kept, removed = [], 0
for r in data:
    if r['e'] in DROP_EQUIP:
        removed += 1
        continue
    if r['e'] == '战绳':
        if r['id'] in ROPE_KEEP:
            r['e'] = '有氧器械'
        else:
            removed += 1
            continue
    if r['e'] == '弹力带' and r['id'] not in BAND_KEEP:
        removed += 1
        continue
    if r['e'] == '徒手' and r['id'] not in BW_KEEP:
        removed += 1
        continue
    if r['id'] in RENAME:
        r['n'] = RENAME[r['id']]
    kept.append(r)

dup = [n for n, c in Counter(r['n'] for r in kept).items() if c > 1]
pc = Counter(r['p'] for r in kept)
ec = Counter(r['e'] for r in kept)

with open(CUR, 'w', encoding='utf-8') as f:
    json.dump(kept, f, ensure_ascii=False, separators=(',', ':'))

print(f'kept {len(kept)} / removed {removed}')
print('parts:', dict(pc))
print('equips:', dict(ec))
print('dups:', len(dup), dup)
