# 动作名全量中文化：
# 1) 后缀与符号修复（male/female/pov、в°乱码）
# 2) 词组表（长词组优先）→ 单词表，覆盖健身术语
# 3) 按 ID 人工覆盖（纯英文名与机翻质量差的名字）
# 4) 打印仍含英文字母的名字（残留清单，加入 OVR 后重跑直到为空）
# 5) 输出重名统计与示例计划名校验
import json
import re
from collections import Counter

NEW = r'C:\Users\till\AppData\Local\Temp\exercises-dataset\data\exercises.json'
CUR = r'D:\FitLog\app\src\main\assets\dataset\exercises.json'

with open(NEW, encoding='utf-8') as f:
    new = json.load(f)
with open(CUR, encoding='utf-8') as f:
    cur = json.load(f)
new_by_id = {r['id']: r for r in new}

# ---------- 词组表（先长后短） ----------
PHRASES = {
    'smith machine': '史密斯机', 'exercise ball': '健身球', 'bosu ball': '波速球',
    'stability ball': '健身球', 'medicine ball': '药球', 'balance board': '平衡板',
    'close grip': '窄距', 'close-grip': '窄距', 'wide grip': '宽距',
    'neutral grip': '中立握', 'overhand grip': '正握', 'underhand grip': '反握',
    'pronated grip': '正握', 'supinated grip': '反握',
    'one arm': '单臂', 'one leg': '单腿', 'one side': '单侧',
    'two arm': '双臂', 'two leg': '双腿', 'both legs': '双腿', 'both arms': '双臂',
    'leg raise': '举腿', 'knee raise': '屈膝举腿', 'leg lift': '举腿', 'hip lift': '提髋',
    'calf raise': '提踵', 'lateral raise': '侧平举', 'front raise': '前平举',
    'lateral pulldown': '高位下拉', 'lat pulldown': '高位下拉',
    'leg curl': '腿弯举', 'leg extension': '腿屈伸',
    'bent over': '俯身', 'straight leg': '直腿', 'straight arm': '直臂', 'straight back': '背部平直',
    'toe touch': '触脚尖', 'heel tap': '点脚跟', 'toe tap': '点脚尖', 'shoulder tap': '点肩',
    'push up': '俯卧撑', 'push-up': '俯卧撑', 'pull up': '引体向上', 'pull-up': '引体向上',
    'chin up': '反手引体', 'chin-up': '反手引体', 'sit up': '仰卧起坐', 'sit-up': '仰卧起坐',
    'step up': '登阶', 'step-up': '登阶', 'bottoms up': '倒持', 'bottoms-up': '倒持',
    'muscle up': '双力臂', 'muscle-up': '双力臂', 'hand stand': '倒立',
    'high knee': '高抬腿', 'high pull': '高拉', 'knee bend': '屈膝',
    'hip thrust': '臀推', 'glute bridge': '臀桥', 'hip bridge': '臀桥',
    'skull crusher': '仰卧臂屈伸', 'skullcrusher': '仰卧臂屈伸', 'french press': '法式推举',
    'push press': '实力举', 'pushdown': '下压', 'kick back': '后屈伸', 'kickback': '后屈伸',
    'body saw': '锯式收腹', 'v up': 'V字两头起', 'v-up': 'V字两头起', 'jackknife': 'V字两头起', 'jack knife': 'V字两头起',
    'l sit': 'L支撑', 'l-sit': 'L支撑', 'v sit': 'V字坐', 'v-sit': 'V字坐',
    'hyper extension': '山羊挺身', 'hyperextension': '山羊挺身',
    'reverse hyper': '反向山羊挺身', 'skin the cat': '单杠翻肩',
    'turkish get up': '土耳其起立', 'turkish get-up': '土耳其起立',
    'renegade row': '匪徒式划船', 'pallof press': '帕洛夫推举',
    'windshield wiper': '雨刮式', 'wind shield wiper': '雨刮式', 'windshield wipers': '雨刮式',
    'hanging leg': '悬垂腿', 'hanging knee': '悬垂屈膝',
    'sumo': '相扑', 'hack': '哈克', 'sissy': '西西里', 'cossack': '哥萨克', 'pistol': '手枪式',
    'curtsey': '屈膝礼', 'landmine': '地雷', 'jefferson': '杰斐逊', 'pendlay': '潘德雷',
    'bradford': '布拉德福德', 'cuban': '古巴式', 'zercher': '泽奇', 'zottman': '佐特曼',
    'tate': '泰特式', 'concentration': '集中', 'preacher': '牧师凳', 'peacher': '牧师凳', 'scott': '牧师凳',
    'hammer': '锤式', 'face pull': '面拉', 'rear delt': '三角肌后束',
    'arm blaster': '臂托板', 'calf blaster': '小腿强化器',
    'close stance': '窄站距', 'wide stance': '宽站距', 'shoulder width': '与肩同宽',
    'jumping jack': '开合跳', 'butt kick': '后踢腿', 'mountain climber': '登山跑',
    'bear crawl': '熊爬', 'crab': '螃蟹式', 'bird dog': '鸟狗式', 'inchworm': '尺蠖爬行',
    'farmers walk': '农夫行走', 'farmer walk': '农夫行走', 'waiter': '侍者式',
    'battle rope': '战绳', 'battling rope': '战绳', 'ski ergometer': '滑雪机',
    'air bike': '风阻单车', 'upright': '直立', 'parallel bar': '双杠',
    'elbow to knee': '肘碰膝', 'knees to elbow': '膝碰肘', 'back and forth': '往返',
    'all fours': '四点支撑', 'against wall': '靠墙', 'on the': '于',
    'good morning': '早安式体前屈', 'power point': '力量点',
    'y raise': 'Y字平举', 'w raise': 'W字平举', 't bar': 'T杆', 'v bar': 'V把', 'v grip': 'V握',
    'ez bar': '曲杆', 'ez-bar': '曲杆', 'step box': '踏箱', 'stepmill': '踏步机',
    'cross over': '交叉', 'crossover': '交叉', 'cross-body': '交叉', 'cross body': '交叉',
    'figure': '8字', 'quarter': '四分之一程', 'half': '半程', 'full': '全程', 'deep': '深度',
    'ski step': '滑雪步', 'ski': '滑雪', 'skier': '滑雪',
    'plank': '平板支撑', 'planche': '俄挺', 'maltese': '马耳他十字', 'human flag': '人体旗',
    'handstand': '倒立', 'headstand': '头倒立', 'muscle': '肌肉', 'gravity': '重力',
    'negative': '离心式', 'kipping': '借力式', 'prisoner': '囚式', 'donkey': '驴式',
    'supine': '仰卧', 'prone': '俯卧', 'reclining': '斜卧', 'sitted': '坐姿', 'seated': '坐姿',
    'kneeling': '跪姿', 'standing': '站姿', 'hanging': '悬垂', 'hang': '悬垂',
    'kneel': '跪姿', 'walk': '行走', 'walking': '行走', 'runners': '跑者', 'run': '跑',
    'sprint': '冲刺', 'march': '踏步', 'hops': '跳', 'hop': '跳', 'jump': '跳',
    'climb': '攀爬', 'crawl': '爬行', 'step': '踏步', 'stride': '步',
    'circles': '绕环', 'circular': '绕环', 'circle': '绕环', 'rotate': '旋转',
    'rotation': '旋转', 'rotational': '旋转', 'rotary': '旋转', 'twisting': '转体',
    'twisted': '转体', 'twist': '转体', 'windmill': '风车', 'wind': '风',
    'wipers': '雨刮式', 'wiper': '雨刮式', 'rocking': '摇摆', 'rocky': '洛奇式',
    'slide': '滑动', 'drag': '拖曳', 'drive': '发力', 'drop': '下落',
    'reach': '延展', 'release': '松放', 'pass': '传递', 'catch': '接球',
    'tap': '点触', 'tilt': '倾斜', 'touch': '触', 'throw': '抛掷',
    'slam': '砸', 'flip': '翻转', 'clap': '击掌', 'hug': '环抱', 'squeeze': '挤压',
    'clasped': '交握', 'hook': '勾拳', 'punch': '出拳', 'boxing': '拳击', 'tennis': '网球',
    'judo': '柔道', 'kayak': '皮划艇', 'swimmer': '游泳式', 'skater': '滑冰者',
    'bicycle': '空中蹬车', 'bike': '单车', 'cycle': '单车', 'cyclist': '单车',
    'elliptical': '椭圆机', 'treadmill': '跑步机', 'stationary': '固定式', 'ergometer': '测功仪',
    'rower': '划船机', 'row': '划船', 'pulley': '滑轮', 'cable': '绳索', 'sled': '雪橇机',
    'lever': '器械', 'machine': '机', 'cage': '架', 'rack': '架', 'pin': '销',
    'platform': '平台', 'pad': '垫', 'bench': '长凳', 'chair': '椅', 'board': '板',
    'bar': '杆', 'bars': '杠', 'cambered': '弓形', 'attachment': '配件', 'handle': '把手',
    'stirrups': '马镫把手', 'strap': '拉带', 'straps': '拉带', 'towel': '毛巾',
    'ball': '球', 'ring': '吊环', 'rope': '绳索', 'tire': '轮胎', 'iron': '铁十字',
    'wheel': '轮', 'roller': '滚轮', 'rollerer': '滚轮', 'rollerout': '健腹轮外推',
    'foam': '泡沫', 'blaster': '强化器', 'gripper': '握力器', 'gripless': '无握把',
    'equipment': '器械', 'trainer': '训练器', 'weight': '负重', 'bodyweight': '自重',
    'touchers': '触', 'fixed': '固定', 'pallof': '帕洛夫', 'low': '低位', 'high': '高位',
    'backward': '后撤', 'battling': '战绳', 'box': '箱', 'down': '下', 'forward': '前',
    'yoga': '瑜伽', 'pro': '专业', 'smith': '史密斯机', 'guillotine': '断头台式',
    'elevated': '高位', 'skull': '碎颅式', 'palm': '掌心', 'palms': '掌心',
    'grip': '握', 'depresor': '降肌', 'scissor': '剪刀式', 'ez': '曲杆',
    'stepbox': '踏箱', 'extended': '伸展', 'above': '头上', 'ups': '上举',
    'off': '离', 'plus': '加强', 'clock': '时钟', 'balance': '平衡',
    'captains': '悬垂凳', 'point': '点', 'response': '反弹', 'greatest': '最伟大',
    'body': '身体', 'ground': '地面', 'floor': '地板', 'wall': '墙', 'staircase': '阶梯',
    'elevator': '电梯式', 'seesaw': '跷跷板', 'pyramid': '金字塔', 'diamond': '钻石式',
    'stork': '鹳式', 'star': '星式', 'flag': '旗式', 'spider': '蜘蛛式', 'cobra': '眼镜蛇式',
    'sphinx': '狮身人面式', 'frog': '蛙式', 'cat': '猫式', 'dog': '犬式', 'gorilla': '大猩猩式',
    'monster': '怪兽式', 'pirate': '海盗式', 'gironda': '吉隆达', 'sternum': '胸骨',
    'korean': '韩式', 'london': '伦敦桥', 'spell caster': '斯佩尔转体', 'spell': '斯佩尔',
    'caster': '转体', 'otis': '奥蒂斯式', 'janda': '扬达式', 'svend': '斯文德式',
    'thibaudeau': '蒂博式', 'frankenstein': '科学怪人式', 'butterfly': '蝴蝶式',
    'butter': '蝴蝶式', 'hindu': '印度式', 'cocoons': '茧式卷腹', 'cocoon': '茧式卷腹',
    'jackknife': 'V字两头起', 'elevator': '电梯式', 'bottoms': '倒持',
    'crab': '螃蟹式', 'stork': '鹳式',
    # 肌群
    'ab': '腹部', 'abdominal': '腹部', 'abduction': '外展', 'abductor': '外展',
    'adduction': '内收', 'adductor': '内收', 'oblique': '腹斜肌', 'rectus': '腹直肌',
    'glute': '臀', 'glutes': '臀', 'gluteus': '臀', 'quad': '股四头', 'quads': '股四头',
    'ham': '腘绳肌', 'hamstring': '腘绳肌', 'femoral': '股', 'femoris': '股',
    'calf': '小腿', 'calves': '小腿', 'tibialis': '胫骨前肌', 'peroneals': '腓骨肌',
    'piriformis': '梨状肌', 'scapula': '肩胛', 'scapular': '肩胛', 'deltoid': '三角肌',
    'lat': '背阔', 'pec': '胸', 'pectoralis': '胸', 'biceps': '二头', 'tricep': '三头',
    'triceps': '三头', 'groin': '腹股沟', 'spine': '脊柱', 'pelvic': '骨盆',
    'flexion': '屈', 'flexor': '屈肌', 'extensor': '伸肌', 'depressor': '降肌',
    'retractor': '收紧', 'external': '外旋', 'internal': '内旋', 'anti': '抗',
    'posterior': '后侧', 'major': '大',
    # 部位/器官
    'arm': '臂', 'arms': '臂', 'leg': '腿', 'legs': '腿', 'hand': '手', 'hands': '手',
    'finger': '手指', 'elbow': '肘', 'knee': '膝', 'knees': '膝', 'keens': '膝',
    'ankle': '踝', 'ankles': '踝', 'wrist': '腕', 'head': '头', 'heel': '脚跟',
    'toe': '脚尖', 'toes': '脚尖', 'feet': '双脚', 'foot': '脚', 'shoulder': '肩',
    'shoulders': '肩', 'hip': '髋', 'hips': '髋', 'back': '背', 'chest': '胸',
    'neck': '颈', 'face': '面部', 'skin': '皮',
    # 方位/程度
    'inner': '内侧', 'outer': '外侧', 'upper': '上', 'lower': '下', 'middle': '中',
    'rear': '后', 'front': '前', 'lateral': '侧', 'medial': '内侧',
    'behind': '体后', 'between': '之间', 'against': '抵', 'across': '横跨',
    'around': '绕', 'over': '过顶', 'overhead': '过顶', 'under': '下方',
    'through': '穿越', 'into': '转入', 'from': '自', 'with': '配', 'on': '上置于',
    'in': '于', 'at': '于', 'to': '至', 'of': '', 'the': '', 'a': '', 'and': '与',
    'apart': '分开', 'astride': '跨坐', 'straddle': '跨坐', 'stance': '站距',
    'position': '位', 'pose': '式', 'style': '式', 'posture': '姿态', 'motion': '活动',
    'range': '幅度', 'angle': '斜角', 'angled': '斜角', 'angled': '斜角', 'diagonal': '对角',
    'vertical': '垂直', 'horizontal': '水平', 'parallel': '平行', 'degrees': '度',
    'depth': '深跳', 'dynamic': '动态', 'static': '静态', 'quick': '快速', 'speed': '速度',
    'power': '爆发力', 'plyo': '增强式', 'stabilization': '稳定', 'support': '支撑',
    'supported': '支撑', 'self': '自我', 'assist': '辅助', 'assisted': '辅助',
    'advanced': '高级', 'intermediate': '中级', 'basic': '基础', 'modified': '变式',
    'variation': '变式', 'sequence': '连续', 'multiple': '多', 'single': '单',
    'double': '双', 'triple': '三', 'three': '三', 'two': '双', 'one': '单',
    'both': '双', 'all': '全', 'left': '左', 'right': '右', 'semi': '半',
    'short': '短', 'long': '长', 'big': '大', 'wide': '宽', 'close': '窄', 'closer': '更窄',
    'open': '开', 'closed': '闭合', 'tuck': '收腿', 'pike': '折刀', 'hollow': '空心',
    'arch': '挺身', 'bent': '屈', 'bend': '屈', 'bends': '屈', 'stiff': '直膝',
    'straight': '直', 'flat': '平', 'round': '圆', 'curved': '弧线',
    'inverted': '倒置', 'inverse': '反向', 'revers': '反向', 'mixed': '混合握',
    'neutral': '中立', 'overhand': '正握', 'underhand': '反握',
    'pronate': '旋前', 'pronated': '正握', 'pronation': '旋前',
    'supinated': '反握', 'supination': '反握', 'unilateral': '单侧',
    'contralateral': '对侧', 'alternating': '交替', 'alternate': '交替',
    'cross': '交叉', 'crossed': '交叉', 'curl': '弯举', 'extension': '屈伸',
    'flex': '屈', 'lift': '举', 'lifting': '举', 'raise': '举', 'raised': '抬高',
    'lowering': '下放', 'pull': '拉', 'push': '推', 'press': '推举',
    'squat': '深蹲', 'squad': '深蹲', 'lunge': '箭步蹲', 'deadlift': '硬拉',
    'crunch': '卷腹', 'situp': '仰卧起坐', 'bridge': '桥式', 'planch': '俄挺',
    'dip': '臂屈伸', 'dips': '臂屈伸', 'shrug': '耸肩', 'fly': '飞鸟',
    'kick': '踢', 'kicks': '踢', 'stretch': '拉伸', 'hold': '静态保持',
    'carry': '提行', 'jerk': '挺举', 'thruster': '蹲推', 'snatch': '抓举',
    'clean': '翻站', 'swing': '摆荡', 'get up': '起立', 'getup': '起立',
    'rock': '摇滚', 'roll': '滚动', 'rolling': '滚动', 'spin': '旋转',
    'turn': '转动', 'lean': '倾', 'tilt': '倾斜', 'shift': '转移',
    'pulse': '小幅度', 'pump': '泵感', 'reps': '次数', 'set': '组',
    'male': '男', 'female': '女', 'pov': '视角', 'version': '版本',
    's': '', 'es': '', 'ed': '', 'th': '', 'sz': '', 'oid': '', 'ge': '', 'ged': '',
    'ting': '', 'v': 'V', 't': 'T', 'l': 'L', 'y': 'Y', 'w': 'W', 'd': 'D', 'jm': 'JM式',
}

# ---------- 按 ID 人工覆盖（纯英文名 / 机翻差名 / 示例计划名） ----------
OVR = {
    '0003': '空中蹬车', '1368': '绕踝', '2355': '悬垂屈膝摆腿', '2333': '悬垂直腿摆腿',
    '3214': '分臂绕环触脚尖（男）', '3672': '往返踏步', '0020': '平衡板', '3212': '基础触脚尖（男）',
    '3360': '熊爬', '0137': '地板臂屈伸', '0138': '仰卧提臀', '0870': '支撑提臀',
    '0260': '茧式卷腹', '1468': '螃蟹式转体触脚尖', '2331': '单车交叉训练器', '0443': '肘碰膝卷腹',
    '3292': '电梯式收腹', '1338': '健身球环抱', '2133': '农夫行走', '3303': '人体旗',
    '3301': '蛙式俄挺', '3315': '全程马耳他十字', '3299': '全程俄挺', '0466': '吉隆达胸骨引体',
    '0467': '大猩猩引体', '3221': '半程屈膝（男）', '2139': '手摇单车', '3218': '交握绕环触脚尖（男）',
    '3302': '倒立', '0473': '悬垂折刀举腿', '3636': '靠墙高抬腿', '1471': '尺蠖爬行',
    '3698': '尺蠖爬行二式', '0555': '坐姿踢腿', '0558': '借力双力臂', '3419': '地板L支撑',
    '0562': '地雷架180°转体', '3300': '倾斜俄挺', '2271': '拳击左勾拳', '0609': '伦敦桥式',
    '0624': '靠墙静蹲踏步', '0628': '怪兽走', '0631': '双力臂', '1401': '双力臂（单杠）',
    '0641': '奥蒂斯挺身', '3147': '骨盆后倾', '1422': '骨盆后倾接臀桥', '1687': '后撤步过头延展',
    '1689': '自重推拉', '3638': '俯卧撑接冲刺跑', '3533': '股四头肌拉伸', '3552': '快速碎步二式',
    '2204': '滚轮锯式收腹', '0685': '原地跑', '0684': '原地跑（器械）', '3656': '短步快跑',
    '3699': '支撑点肩', '3361': '滑冰跳', '2142': '滑雪机', '3671': '滑雪步',
    '3304': '单杠翻肩', '0777': '斯佩尔转体', '1362': '狮身人面式', '2329': '脊柱转体',
    '2138': '固定单车冲刺三式', '0798': '固定单车慢骑', '3314': '分腿马耳他十字',
    '3298': '分腿俄挺', '1427': '直腿髋外展', '3433': '游泳式打腿二式（男）', '2459': '翻轮胎',
    '1466': '转体提髋', '3231': '双腿触脚尖（男）', '1366': '上犬式', '3420': '地板V字坐',
    '2141': '椭圆机行走', '2311': '踏步机行走', '0857': '健腹轮外推', '3637': '健腹轮滑行',
    '0858': '变速冲刺跑', '1428': '绕腕', '0859': '腕滚轮',
    # 机翻质量差 / 示例计划引用名
    '1269': '绳索直立龙门架夹胸', '0007': '交替侧下拉',
    '0006': '交替触脚跟', '0010': '辅助悬垂屈膝举腿抛球', '0012': '辅助仰卧举腿侧抛球',
    '0013': '辅助仰卧举腿抛球', '1473': '后撤跳',
    '3117': '弹力带固定窄距下拉', '3116': '弹力带固定反握下拉', '1369': '弹力带双腿提踵二式',
    '0029': '杠铃前蹲（肩宽握）', '0035': '杠铃下斜窄距仰卧臂屈伸', '0045': '杠铃断头台式卧推',
    '0048': '杠铃上斜反握推举', '1411': '杠铃掌心向下腕弯举', '1412': '杠铃掌心向上腕弯举',
    '1374': '跳箱落地单腿稳定', '1494': '蝴蝶式拉伸', '0197': '绳索下拉（专业背阔杆）',
    '0225': '绳索站姿十字过顶反向飞鸟', '0862': '绳索上下转体', '1325': '绳索宽距颈后下拉',
    '0981': '弹力带V字两头起',
    '2203': '滚轮坐姿肩胛激活', '2209': '滚轮坐姿单腿肩胛激活', '0720': '左右侧反手引体',
    '3645': '单腿桥式（另一腿伸直）', '3016': '卷腹', '2963': '悬垂凳直腿举腿',
    '1548': '椅子伸腿拉伸', '1651': '哑铃弓步弯举', '0311': '哑铃满罐式侧平举',
    '0316': '哑铃上斜推举', '2143': '哑铃站姿大绕环', '3234': '哑铃高位飞鸟',
    '0543': '壶铃海盗式摆腿', '1576': '抬腿腘绳肌拉伸', '1303': '药球胸推（三点站姿）',
    '1304': '药球胸推（多次反弹）', '1305': '药球胸推（单次反弹）', '0660': '哑铃窄距俯卧撑',
    '3145': '俯卧撑加强式', '0739': '雪橇机 45° 腿举',
    '3289': '高难臂屈伸', '0521': '壶铃交替匪徒式划船', '3119': '如厕式深蹲',
    '3132': '如厕式深蹲（支撑）', '1423': '反向山羊挺身（置于平凳）', '3291': '回环推举',
    '0805': '悬挂腹部展开', '0818': '双把手平行握背阔下拉', '0845': '负重俄罗斯转体（抬腿）',
    '1604': '世界最伟大拉伸',
}
# 按英文名兜底覆盖
OVR_EN = {'barbell full squat': '杠铃全蹲'}


def fix_suffix(n: str) -> str:
    n = n.replace('в°', '°').replace('в', '°').replace('（(', '（').replace(')）', '）')
    for a, b in (('（male）', '（男）'), ('（female）', '（女）'), ('（back pov）', '（背面视角）'),
                 ('（side pov）', '（侧面视角）'), ('（front pov）', '（正面视角）'),
                 ('(male)', '（男）'), ('(female)', '（女）'), ('(back pov)', '（背面视角）'),
                 ('(side pov)', '（侧面视角）'), ('(front pov)', '（正面视角）')):
        n = n.replace(a, b)
    # "v. 2/3/4" 版本号 → 二式/三式/四式
    n = re.sub(r'[Vv]\.\s*([234])', lambda m: {2: '二式', 3: '三式', 4: '四式'}[int(m.group(1))], n)
    # "（on X）" / 句尾 " on X" → （置于X）
    n = re.sub(r'[（(]\s*on\s+([^）)]*)[）)]', r'（置于\1）', n)
    n = re.sub(r'\bon\s+([^（）()]+)$', r'（置于\1）', n.strip())
    # 机翻残留 "反向－grip" → 反向握
    n = n.replace('－grip', '握').replace('-grip', '握')
    return n


def sub_tok(n: str) -> str:
    # 词组优先（按长度降序，词边界替换）
    for p in sorted(PHRASES, key=len, reverse=True):
        n = re.sub(r'(?<![A-Za-z])' + re.escape(p) + r'(?![A-Za-z])', PHRASES[p], n, flags=re.I)
    return n


def clean(n: str) -> str:
    n = re.sub(r'\s+', ' ', n).strip()
    n = n.replace('（ ', '（').replace(' ）', '）').strip()
    n = re.sub(r'（\s*）', '', n)
    # 修正常见机翻残留（半成品名 + 英文碎片）
    for a, b in (('V-杆', 'V把'), ('bosu球', '波速球'), ('绳索绳索', '绳索'),
                 ('Y－', 'Y字'), ('绳索侧下拉', '绳索高位下拉'),
                 ('french 推举', '法式推举'), ('掌心 up', '掌心向上'), ('掌心 down', '掌心向下'),
                 ('宽距 grip', '宽距'), ('窄距 grip', '窄距'), ('平行 grip', '对握'),
                 ('锤式 grip', '锤式握'), ('反向 grip', '反握'), ('混合握 grip', '混合握'),
                 ('肩 grip', '肩宽握'), ('（高翻 grip）', '（翻站握距）'), ('ez－杠铃', '曲杆'),
                 ('above 头', '过头顶'), ('out 侧', '外侧'), ('out 拉伸腿', '伸直腿'),
                 ('hyper 屈伸', '山羊挺身'), ('jack 波比跳', '开合波比跳'), ('jack 跳跃', '开合跳'),
                 ('臂 ups', '臂上举'), ('上置于', '置于'), ('45 度', '45度'), ('单腿举', '单腿腿举')):
        n = n.replace(a, b)
    # 去掉汉字之间的空格
    n = re.sub(r'(?<=[\u4e00-\u9fff%）])\s+(?=[\u4e00-\u9fff（0-9])', '', n)
    n = re.sub(r'\s+', ' ', n).strip()
    return n


changed = 0
for r in cur:
    u = new_by_id[r['id']]
    n = fix_suffix(r['n'])
    n = sub_tok(n)
    n = clean(n)
    if r['id'] in OVR:
        n = OVR[r['id']]
    elif u['name'] in OVR_EN:
        n = OVR_EN[u['name']]
    if n != r['n']:
        changed += 1
    r['n'] = n

# 残留检查：2 个及以上连续字母视为残留（JM 为合法术语）
residual = [(r['id'], r['en'], r['n']) for r in cur
            if re.search(r'[A-Za-z]{2,}', r['n']) and 'JM' not in r['n']]
dup = [n for n, c in Counter(r['n'] for r in cur).items() if c > 1]

DEMO = ['杠铃卧推', '器械推胸', '哑铃侧平举', '绳索直立龙门架夹胸', '俯卧撑',
        '引体向上', '交替侧下拉', '绳索坐姿划船', '杠铃弯举',
        '杠铃全蹲', '雪橇机 45° 腿举', '杠铃臀桥', '登山跑']
names = {r['n'] for r in cur}
missing = [d for d in DEMO if d not in names]

print('renamed:', changed)
print('residual latin names:', len(residual))
for i, en, n in residual[:60]:
    print(f'  {i} | {en} | {n}')
print('duplicates after rename:', len(dup), dup[:20])
print('demo missing:', missing)

if not residual and not missing:
    with open(CUR, 'w', encoding='utf-8') as f:
        json.dump(cur, f, ensure_ascii=False, separators=(',', ':'))
    import os
    print('WROTE', CUR, round(os.path.getsize(CUR) / 1048576, 2), 'MB')
