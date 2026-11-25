#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 sql/init.sql：结构(schema.sql) + 真实分布的测试数据。
数据量：套餐 20 / 预约 32 / 检查项目 30 / 时段 30 / 团检批次 6 / 名单 60 / 拥挤度计算 34
时间跨度：2026-05 ~ 2026-11，按业务状态倒推，保证状态机与关联字段自洽。
"""
import json
import pathlib
from datetime import date, datetime, timedelta

HERE = pathlib.Path(__file__).parent
DB = "new_035"
SCHEMA = (HERE / "schema.sql").read_text(encoding="utf-8").replace("__DB__", DB)

A = []          # 输出的 SQL 片段
def w(s=""):
    A.append(s)

def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, (int, float)):
        return str(v)
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"

def insert(table, cols, rows, chunk=200):
    if not rows:
        return
    for i in range(0, len(rows), chunk):
        part = rows[i:i + chunk]
        w(f"INSERT INTO `{table}` ({', '.join('`%s`' % c for c in cols)}) VALUES")
        vals = ",\n".join("(" + ", ".join(q(x) for x in r) + ")" for r in part)
        w(vals + ";")
        w()

def dt(d, h=9, mi=0):
    if isinstance(d, datetime):
        return d.strftime("%Y-%m-%d %H:%M:%S")
    return f"{d.isoformat()} {h:02d}:{mi:02d}:00"

TODAY = date(2026, 11, 1)

# ---------------------------------------------------------------- 组织
orgs = [
    ("华远建设集团", "ENTERPRISE", "陈静", "021-64881200"),
    ("启明软件有限公司", "ENTERPRISE", "赵磊", "021-64883311"),
    ("市第一人民医院后勤处", "ENTERPRISE", "孙倩", "021-64889900"),
    ("恒达物流股份有限公司", "ENTERPRISE", "黄伟", "021-64887755"),
    ("青禾教育集团", "ENTERPRISE", "周敏", "021-64886622"),
    ("康检通体检中心", "HOSPITAL", "王丽", "021-64880000"),
]
insert("sys_org", ["org_name", "org_type", "contact", "phone", "create_time"],
       [(o[0], o[1], o[2], o[3], dt(date(2026, 1, 8) + timedelta(days=i * 9), 9, 30)) for i, o in enumerate(orgs)])

# ---------------------------------------------------------------- 角色
roles = [
    ("ADMIN", "体检管理员", "ALL", "维护套餐、项目、时段容量、团检、报告和统计"),
    ("FRONT", "前台工作人员", "DEPT", "审核预约、登记收费、到检、补打指引单和报告交付"),
    ("ENTERPRISE", "企业联络员", "ORG", "建立团检批次、导入名单、查看集合统计"),
    ("USER", "体检用户", "SELF", "查看套餐、预约、改期、填写问卷、领取报告"),
]
insert("sys_role", ["role_code", "role_name", "data_scope", "remark", "create_time"],
       [(r[0], r[1], r[2], r[3], dt(date(2026, 1, 5), 10, 0)) for r in roles])

# ---------------------------------------------------------------- 用户
# BCrypt(admin123) / BCrypt(123456)，cost=10
H_ADMIN = "$2b$10$39LJ7d4n0EUOwCE8RaH4huaMmcOdaPt14aokFMSwegITT6rVhmyBa"
H_USER = "$2b$10$3zH9ayX/KrZVuzgHpBYbcuYnW4CCFkBFybQVMHWJfXQIw04CWS.VO"

users = [
    ("admin", H_ADMIN, "系统管理员", "M", "13900000001", "310101********0000", 1, "ADMIN", 6, "信息科"),
    ("mgr01", H_USER, "王丽", "F", "13900000002", "310101********0012", 2, "ADMIN", 6, "体检中心"),
    ("front01", H_USER, "王敏", "F", "13900000003", "310101********0024", 3, "FRONT", 6, "前台"),
    ("front02", H_USER, "刘洋", "M", "13900000004", "310101********0036", 4, "FRONT", 6, "前台"),
    ("ent01", H_USER, "陈静", "F", "13900000005", "310101********0048", 1, "ENTERPRISE", 1, "人力资源部"),
    ("ent02", H_USER, "赵磊", "M", "13900000006", "310101********0051", 2, "ENTERPRISE", 2, "行政部"),
    ("user01", H_USER, "张伟", "M", "13800006621", "310101********0063", 1, "USER", None, None),
    ("user02", H_USER, "李娜", "F", "13800006622", "310101********0075", 2, "USER", None, None),
    ("user03", H_USER, "王强", "M", "13800006623", "310101********0087", 3, "USER", None, None),
    ("user04", H_USER, "刘芳", "F", "13800006624", "310101********0099", 4, "USER", None, None),
    ("user05", H_USER, "陈明", "M", "13800006625", "310101********0102", 5, "USER", None, None),
    ("user06", H_USER, "赵雪", "F", "13800006626", "310101********0114", 6, "USER", None, None),
]
INSERT_DT = date(2026, 2, 10)
insert("sys_user",
       ["username", "password", "real_name", "gender", "phone", "id_card_masked", "avatar",
        "role_code", "org_id", "dept", "status", "last_login_time", "create_time"],
       [(u[0], u[1], u[2], u[3], u[4], u[5], f"/uploads/seed/avatar/{u[6]:02d}.jpg",
         u[7], u[8], u[9], 1,
         dt(INSERT_DT + timedelta(days=min(60 + i * 21, 255)), 8, 40),
         dt(INSERT_DT + timedelta(days=i * 11), 9, 12)) for i, u in enumerate(users)])

# ---------------------------------------------------------------- 字典
dicts = []
for i, (label, val) in enumerate([("待确认", "PENDING_CONFIRM"), ("待缴费", "PENDING_PAY"),
                                  ("待到检", "WAIT_CHECKIN"), ("体检中", "CHECKING"),
                                  ("待报告", "WAIT_REPORT"), ("待领取", "WAIT_PICKUP"),
                                  ("已完成", "COMPLETED"), ("已取消", "CANCELLED"), ("已改期", "RESCHEDULED")], 1):
    dicts.append(("appointment_status", label, val, i, 1))
for i, (label, val) in enumerate([("现金", "CASH"), ("微信", "WECHAT"), ("支付宝", "ALIPAY"),
                                  ("银行卡", "CARD"), ("单位统一结算", "GROUP")], 1):
    dicts.append(("pay_method", label, val, i, 1))
for i, (label, val) in enumerate([("本人下载", "ONLINE"), ("现场领取", "SELF"), ("委托代领", "ENTRUST")], 1):
    dicts.append(("pickup_type", label, val, i, 1))
for i, (label, val) in enumerate([("内科", "内科"), ("外科", "外科"), ("眼科", "眼科"), ("耳鼻喉科", "耳鼻喉科"),
                                  ("口腔科", "口腔科"), ("检验科", "检验科"), ("影像科", "影像科"),
                                  ("妇科", "妇科"), ("功能科", "功能科")], 1):
    dicts.append(("dept", label, val, i, 1))
insert("sys_dict", ["dict_type", "dict_label", "dict_value", "sort", "status"], dicts)

# ---------------------------------------------------------------- 菜单
menus = [
    (0, "概览", None, "▤", None, "M", 1),
    (1, "运营看板", "/dashboard", "▤", "dashboard:view", "C", 1),
    (0, "主数据", None, "◇", None, "M", 2),
    (3, "体检套餐", "/package", "◇", "package:list", "C", 1),
    (3, "检查项目", "/item", "◈", "item:list", "C", 2),
    (3, "时段容量", "/slot", "▣", "slot:list", "C", 3),
    (3, "体检机构", "/org", "◉", "org:list", "C", 4),
    (0, "业务办理", None, "▷", None, "M", 3),
    (8, "预约管理", "/appointment", "▷", "appointment:list", "C", 1),
    (8, "团检管理", "/group", "◎", "group:list", "C", 2),
    (8, "报告归档", "/report", "▦", "report:list", "C", 3),
    (8, "报告领取", "/pickup", "◐", "pickup:list", "C", 4),
    (0, "创新模块", None, "⚙", None, "M", 4),
    (13, "拥挤度规则", "/crowd-rule", "⚙", "crowd:rule", "C", 1),
    (13, "计算留痕", "/crowd-score", "▥", "crowd:score", "C", 2),
    (0, "系统管理", None, "☰", None, "M", 5),
    (16, "菜单管理", "/system/menu", "☰", "menu:list", "C", 1),
    (16, "角色管理", "/system/role", "◫", "role:list", "C", 2),
    (16, "用户管理", "/system/user", "☺", "user:list", "C", 3),
    (0, "消息中心", "/message", "✉", "message:list", "C", 9),
    (0, "体检套餐门户", "/portal/packages", "◇", "portal:package", "C", 11),
    (0, "我的预约", "/portal/appointments", "▷", "portal:appointment", "C", 12),
    (0, "我的报告", "/portal/reports", "▦", "portal:report", "C", 13),
    (0, "个人中心", "/profile", "☺", "profile:view", "C", 14),
    (0, "前台工作", None, "▣", None, "M", 6),
    (25, "到检执行台", "/front/checkin", "▣", "front:checkin", "C", 1),
    (25, "收费登记", "/front/payment", "¥", "front:payment", "C", 2),
    (25, "预约审核", "/front/audit", "▤", "front:audit", "C", 3),
    (0, "企业团检", None, "◎", None, "M", 7),
    (29, "团检批次", "/enterprise/batch", "◎", "ent:batch", "C", 1),
    (29, "名单导入", "/enterprise/member", "⇪", "ent:member", "C", 2),
]
menu_rows = []
for i, (pid, name, path, icon, perm, mtype, sort) in enumerate(menus, 1):
    menu_rows.append((pid, name, path, icon, perm, mtype, sort, 1, dt(date(2026, 1, 6), 10, i % 60)))
insert("sys_menu", ["parent_id", "menu_name", "path", "icon", "perm", "menu_type", "sort", "visible", "create_time"], menu_rows)

ROLE_MENU = {
    "ADMIN": list(range(1, 25)),
    "FRONT": [20, 24, 25, 26, 27, 28, 9],
    "ENTERPRISE": [20, 24, 29, 30, 31],
    "USER": [20, 21, 22, 23, 24],
}
rm = []
for rid, (code, _, _, _) in enumerate(roles, 1):
    for mid in ROLE_MENU[code]:
        rm.append((rid, mid))
insert("sys_role_menu", ["role_id", "menu_id"], rm)

# ---------------------------------------------------------------- 检查项目（30）
ITEMS = [
    ("血常规", "检验科", "无需空腹", "ALL", 0, 120, 28, 10, "采血后按压 5 分钟", "DEV-LAB-01"),
    ("肝功能八项", "检验科", "空腹 8 小时", "ALL", 0, 120, 96, 10, "检查前一晚忌饮酒", "DEV-LAB-01"),
    ("肾功能四项", "检验科", "空腹 8 小时", "ALL", 0, 120, 88, 10, "避免剧烈运动", "DEV-LAB-01"),
    ("血脂四项", "检验科", "空腹 12 小时", "ALL", 0, 120, 76, 10, "素食三天更准确", "DEV-LAB-01"),
    ("空腹血糖", "检验科", "空腹 8 小时", "ALL", 0, 120, 18, 5, "晨起勿进食", "DEV-LAB-01"),
    ("糖化血红蛋白", "检验科", "无需空腹", "ALL", 0, 120, 68, 10, "反映近 3 个月血糖", "DEV-LAB-02"),
    ("尿常规", "检验科", "留取中段尿", "ALL", 0, 120, 22, 10, "避开月经期", "DEV-LAB-03"),
    ("甲状腺功能三项", "检验科", "空腹 8 小时", "ALL", 0, 120, 168, 10, "如服药请告知医生", "DEV-LAB-02"),
    ("肿瘤标志物五项", "检验科", "空腹 8 小时", "ALL", 30, 120, 320, 10, "仅作辅助参考", "DEV-LAB-02"),
    ("同型半胱氨酸", "检验科", "空腹 8 小时", "ALL", 30, 120, 88, 10, "心脑血管风险指标", "DEV-LAB-02"),
    ("内科常规检查", "内科", "无需准备", "ALL", 0, 120, 40, 15, "如实告知既往病史", "DEV-ROOM-01"),
    ("外科常规检查", "外科", "无需准备", "ALL", 0, 120, 40, 15, "含脊柱四肢检查", "DEV-ROOM-02"),
    ("心电图", "功能科", "静息 5 分钟", "ALL", 0, 120, 36, 10, "检查时保持平静", "DEV-ECG-01"),
    ("肺功能检测", "功能科", "无需空腹", "ALL", 20, 120, 88, 15, "检查前勿吸烟", "DEV-PFT-01"),
    ("骨密度检测", "功能科", "无需准备", "ALL", 40, 120, 120, 15, "孕妇不建议", "DEV-BMD-01"),
    ("动脉硬化检测", "功能科", "静息 10 分钟", "ALL", 40, 120, 110, 15, "避免咖啡因", "DEV-AS-01"),
    ("腹部彩超", "影像科", "空腹 8 小时", "ALL", 0, 120, 150, 20, "检查前勿进食产气食物", "DEV-US-01"),
    ("泌尿系彩超", "影像科", "需憋尿", "ALL", 0, 120, 130, 15, "检查前饮水 500ml", "DEV-US-02"),
    ("甲状腺彩超", "影像科", "无需准备", "ALL", 18, 120, 140, 15, "无需空腹", "DEV-US-02"),
    ("颈动脉彩超", "影像科", "无需准备", "ALL", 40, 120, 180, 15, "高血压人群建议", "DEV-US-03"),
    ("心脏彩超", "影像科", "无需准备", "ALL", 30, 120, 220, 20, "需提前预约", "DEV-US-03"),
    ("胸部正位 DR", "影像科", "去除金属饰品", "ALL", 18, 120, 68, 10, "孕妇禁做", "DEV-DR-01"),
    ("胸部低剂量 CT", "影像科", "去除金属饰品", "ALL", 35, 120, 320, 15, "孕妇禁做", "DEV-CT-01"),
    ("头颅 CT", "影像科", "无需准备", "ALL", 40, 120, 380, 15, "体内有金属植入物请告知", "DEV-CT-01"),
    ("乳腺彩超", "影像科", "无需准备", "F", 25, 65, 160, 15, "建议月经结束后检查", "DEV-US-04"),
    ("妇科检查", "妇科", "避开月经期", "F", 20, 65, 80, 15, "检查前 24 小时避免同房", "DEV-GYN-01"),
    ("宫颈 TCT", "妇科", "避开月经期", "F", 21, 65, 180, 10, "取材后少量出血正常", "DEV-GYN-01"),
    ("HPV 分型检测", "妇科", "避开月经期", "F", 21, 65, 320, 10, "与 TCT 联合筛查", "DEV-GYN-01"),
    ("眼科常规检查", "眼科", "无需准备", "ALL", 0, 120, 46, 10, "含视力与眼底", "DEV-EYE-01"),
    ("口腔科检查", "口腔科", "无需准备", "ALL", 0, 120, 40, 10, "含牙周检查", "DEV-DEN-01"),
]
item_rows = []
for i, it in enumerate(ITEMS, 1):
    item_rows.append((f"IT-{i:03d}", it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], it[9],
                      1, dt(date(2026, 1, 12) + timedelta(days=i * 3), 9, (i * 7) % 60)))
insert("exam_item",
       ["item_no", "item_name", "dept", "pre_requirement", "gender_limit", "age_min", "age_max",
        "ref_price", "duration_min", "notice", "device_code", "status", "create_time"], item_rows)

# ---------------------------------------------------------------- 套餐（20）
PACKAGES = [
    ("PKG-2026-001", "入职体检基础套餐", "18-45 岁", 12, [1, 2, 5, 7, 11, 12, 13, 22, 29, 30, 8, 6], 320, 268, 60, "ON_SHELF"),
    ("PKG-2026-002", "中青年常规体检 A", "25-50 岁", 18, [1, 2, 3, 4, 5, 7, 11, 12, 13, 17, 22, 19, 29, 30, 8, 9, 10, 15], 698, 586, 90, "ON_SHELF"),
    ("PKG-2026-003", "中老年深度筛查套餐", "50 岁以上", 26, list(range(1, 27)), 1520, 1280, 150, "ON_SHELF"),
    ("PKG-2026-004", "女性专项筛查套餐", "25-55 岁女性", 21, [1, 2, 3, 4, 5, 7, 8, 11, 12, 13, 17, 18, 19, 25, 26, 27, 28, 29, 30, 15, 21], 1100, 960, 110, "ON_SHELF"),
    ("PKG-2026-005", "团检基础套餐（企业）", "18-60 岁", 14, [1, 2, 3, 5, 7, 11, 12, 13, 17, 22, 29, 30, 8, 10], 420, 350, 75, "ON_SHELF"),
    ("PKG-2026-006", "入职体检升级套餐", "18-45 岁", 16, [1, 2, 3, 4, 5, 7, 11, 12, 13, 17, 22, 29, 30, 8, 15, 6], 460, 398, 80, "ON_SHELF"),
    ("PKG-2026-007", "中青年常规体检 B", "25-50 岁", 22, [1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 17, 18, 19, 22, 29, 30, 8, 21], 880, 760, 110, "ON_SHELF"),
    ("PKG-2026-008", "中老年心脑血管专项", "55 岁以上", 18, [1, 2, 3, 4, 5, 9, 10, 11, 13, 16, 19, 21, 23, 24, 29, 30, 15, 8], 1180, 980, 120, "ON_SHELF"),
    ("PKG-2026-009", "女性两癌筛查套餐", "30-60 岁女性", 12, [1, 11, 17, 25, 26, 27, 28, 19, 29, 30, 8, 12], 760, 620, 90, "ON_SHELF"),
    ("PKG-2026-010", "男性专项筛查套餐", "35-60 岁男性", 15, [1, 2, 3, 4, 11, 12, 17, 18, 22, 23, 29, 30, 8, 9, 10], 890, 780, 100, "ON_SHELF"),
    ("PKG-2026-011", "公务员体检标准套餐", "18-35 岁", 20, [1, 2, 3, 4, 5, 7, 11, 12, 13, 14, 17, 18, 22, 23, 29, 30, 8, 21, 6, 15], 560, 480, 95, "ON_SHELF"),
    ("PKG-2026-012", "糖尿病风险筛查", "40 岁以上", 13, [1, 2, 3, 4, 5, 6, 7, 11, 17, 19, 29, 30, 8], 680, 590, 85, "ON_SHELF"),
    ("PKG-2026-013", "肝胆专项检查套餐", "30 岁以上", 11, [1, 2, 3, 4, 11, 17, 22, 29, 30, 8, 21], 520, 450, 70, "ON_SHELF"),
    ("PKG-2026-014", "甲状腺专项套餐", "25 岁以上", 9, [1, 7, 8, 11, 12, 19, 29, 30, 13], 380, 320, 55, "ON_SHELF"),
    ("PKG-2026-015", "老年人综合评估套餐", "65 岁以上", 24, [1, 2, 3, 4, 5, 7, 8, 11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 29, 30, 8, 9, 10], 1680, 1420, 160, "ON_SHELF"),
    ("PKG-2026-016", "青少年体检套餐", "12-18 岁", 10, [1, 5, 7, 11, 12, 13, 22, 29, 30, 8], 260, 220, 50, "ON_SHELF"),
    ("PKG-2026-017", "高端全面体检套餐", "30-65 岁", 28, list(range(1, 29)), 3280, 2680, 180, "ON_SHELF"),
    ("PKG-2026-018", "职业健康体检套餐（粉尘）", "20-60 岁", 15, [1, 2, 3, 5, 11, 12, 13, 14, 17, 22, 23, 29, 30, 8, 15], 480, 420, 85, "DRAFT"),
    ("PKG-2026-019", "团检女性套餐（企业）", "20-55 岁女性", 16, [1, 2, 5, 7, 11, 12, 13, 17, 19, 25, 26, 27, 29, 30, 8, 15], 520, 450, 95, "DRAFT"),
    ("PKG-2026-020", "术后康复随访套餐", "30 岁以上", 12, [1, 2, 3, 5, 11, 17, 22, 23, 29, 30, 8, 21], 620, 540, 70, "OFF_SHELF"),
]
pkg_rows, pkg_item_rows = [], []
DEPT_BY_ITEM = {i: it[1] for i, it in enumerate(ITEMS, 1)}
for i, p in enumerate(PACKAGES, 1):
    p = p[:4] + (list(dict.fromkeys(p[4])),) + p[5:]   # 去重但保持顺序
    PACKAGES[i - 1] = p
    depts = sorted({DEPT_BY_ITEM[x] for x in p[4]})
    pkg_rows.append((p[0], p[1], p[2], " / ".join(depts), p[5], p[6], p[7], len(p[4]),
                     f"/uploads/seed/package/{((i - 1) * 7 % 8) + 1:02d}.jpg",
                     "请提前 10 分钟到前台核验预约码；体检当日需空腹，可少量饮水。",
                     date(2026, 1, 1), date(2026, 12, 31), p[8], 1,
                     dt(date(2026, 1, 15) + timedelta(days=i * 5), 10, (i * 11) % 60)))
    for s, iid in enumerate(p[4], 1):
        pkg_item_rows.append((i, iid, s))
insert("exam_package",
       ["package_no", "package_name", "crowd", "dept_summary", "original_price", "discount_price",
        "duration_min", "item_count", "cover_image", "booking_notice", "shelf_start", "shelf_end",
        "status", "version", "create_time"], pkg_rows)
insert("exam_package_item", ["package_id", "item_id", "sort"], pkg_item_rows)

# ---------------------------------------------------------------- 时段容量（未来 10 天 × 3 段）
PERIODS = [("AM_0730", "07:30-09:00", 50, 20, 280), ("AM_0900", "09:00-10:30", 50, 20, 260), ("AM_1030", "10:30-11:30", 30, 10, 180)]
slot_rows = []
slot_ids = {}
sid = 0
for d in range(0, 10):
    day = TODAY + timedelta(days=2 + d)
    for pi, (code, rng, pq, gq, cap) in enumerate(PERIODS):
        sid += 1
        slot_ids[(day, code)] = sid
        load = [38, 76, 94][pi] + (d % 3) * 4
        pb = min(pq, round(pq * load / 100))
        gb = min(gq, round(gq * (load - 8) / 100)) if load > 20 else 0
        slot_rows.append((day, code, rng, pq, gq, pb, max(gb, 0), cap,
                          "CLOSED" if pb + gb >= pq + gq - 3 else "OPEN",
                          dt(TODAY - timedelta(days=6), 9, 0)))
insert("appointment_slot",
       ["slot_date", "period", "time_range", "personal_quota", "group_quota", "personal_booked",
        "group_booked", "dept_capacity", "status", "create_time"], slot_rows)

# ---------------------------------------------------------------- 团检批次（6）
BATCHES = [
    ("GB-2026-011", 1, "华远建设集团", 5, date(2026, 11, 5), date(2026, 11, 20), 120, 118, 0, "PENDING"),
    ("GB-2026-009", 2, "启明软件有限公司", 2, date(2026, 10, 8), date(2026, 10, 22), 80, 76, 71, "DONE"),
    ("GB-2026-007", 3, "市第一人民医院后勤处", 1, date(2026, 8, 12), date(2026, 8, 30), 45, 45, 43, "DONE"),
    ("GB-2026-010", 4, "恒达物流股份有限公司", 18, date(2026, 10, 20), date(2026, 11, 8), 60, 52, 34, "CHECKING"),
    ("GB-2026-008", 5, "青禾教育集团", 6, date(2026, 9, 1), date(2026, 9, 25), 38, 38, 36, "DONE"),
    ("GB-2026-012", 1, "华远建设集团", 19, date(2026, 11, 15), date(2026, 11, 30), 40, 12, 0, "PENDING"),
]
batch_rows = []
for i, b in enumerate(BATCHES, 1):
    batch_rows.append((b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8],
                       orgs[b[1] - 1][2], orgs[b[1] - 1][3], b[9], 5,
                       dt(b[4] - timedelta(days=18), 10, 20), dt(b[4] - timedelta(days=2), 15, 6)))
insert("group_batch",
       ["batch_no", "org_id", "org_name", "package_id", "start_date", "end_date", "quota",
        "joined_count", "checked_count", "contact_name", "contact_phone", "status",
        "create_by", "create_time", "update_time"], batch_rows)

# 名单：批次 1 共 18 行（含 3 行导入失败），其余批次各若干
NAMES = ["张建国", "李秀英", "王志强", "刘丽华", "陈国庆", "杨桂芳", "赵永军", "黄敏",
         "周文斌", "吴小红", "徐海涛", "孙美玲", "马俊", "朱晓燕", "胡建军", "林岚",
         "郭鹏", "何静", "高伟", "罗雪"]
member_rows = []
mid = 0
for bi, b in enumerate(BATCHES, 1):
    cnt = 18 if bi == 1 else (12 if bi == 2 else 8)
    for k in range(cnt):
        mid += 1
        name = NAMES[(bi * 3 + k) % len(NAMES)]
        failed = bi == 1 and k in (17, 11, 4)
        err = None
        if failed:
            err = {17: "身份证号校验位错误", 11: "手机号格式不合法", 4: "套餐编号 PKG-2026-099 不存在"}[k]
        member_rows.append((bi, name, "M" if k % 2 == 0 else "F", 26 + (k * 3) % 30,
                            f"137{bi}{k:04d}88"[:11], f"31010119900{k:02d}{mid:04d}",
                            b[3], "FAILED" if failed else "SUCCESS", err,
                            dt(b[4] - timedelta(days=15 - k % 10), 10, (k * 7) % 60)))
insert("group_member",
       ["batch_id", "member_name", "gender", "age", "phone", "id_card", "package_id",
        "import_status", "error_msg", "create_time"], member_rows)

# ---------------------------------------------------------------- 预约（32）
# (日期, 套餐, 用户, 状态, 时段, 领取方式, 支付方式)
APPS = [
    (date(2026, 11, 3), 3, 7, "WAIT_CHECKIN", "AM_0730", None, "PAID"),
    (date(2026, 11, 3), 2, 8, "WAIT_CHECKIN", "AM_0900", None, "PAID"),
    (date(2026, 11, 4), 4, 10, "WAIT_PAY", "AM_0730", None, "UNPAID"),
    (date(2026, 11, 5), 1, 11, "PENDING_CONFIRM", "AM_1030", None, "UNPAID"),
    (date(2026, 11, 5), 5, 12, "WAIT_CHECKIN", "AM_0730", None, "PAID"),
    (date(2026, 11, 6), 7, 7, "WAIT_CHECKIN", "AM_0900", None, "PAID"),
    (date(2026, 11, 6), 9, 9, "WAIT_PAY", "AM_1030", None, "UNPAID"),
    (date(2026, 11, 9), 15, 10, "WAIT_CHECKIN", "AM_0730", None, "PAID"),
    (date(2026, 10, 29), 2, 8, "CHECKING", "AM_0730", None, "PAID"),
    (date(2026, 10, 28), 11, 11, "CHECKING", "AM_0900", None, "PAID"),
    (date(2026, 10, 26), 3, 12, "CHECKING", "AM_0730", None, "PAID"),
    (date(2026, 10, 22), 3, 7, "WAIT_REPORT", "AM_0900", None, "PAID"),
    (date(2026, 10, 21), 7, 9, "WAIT_REPORT", "AM_0730", None, "PAID"),
    (date(2026, 10, 20), 2, 8, "WAIT_PICKUP", "AM_0900", "ONLINE", "PAID"),
    (date(2026, 10, 19), 11, 10, "WAIT_REPORT", "AM_1030", None, "PAID"),
    (date(2026, 10, 15), 4, 10, "WAIT_PICKUP", "AM_0730", "SELF", "PAID"),
    (date(2026, 10, 14), 12, 11, "COMPLETED", "AM_0900", "ONLINE", "PAID"),
    (date(2026, 10, 12), 8, 12, "WAIT_REPORT", "AM_0730", None, "PAID"),
    (date(2026, 9, 25), 9, 9, "COMPLETED", "AM_0900", "SELF", "PAID"),
    (date(2026, 9, 18), 3, 10, "COMPLETED", "AM_0730", "ENTRUST", "PAID"),
    (date(2026, 9, 15), 4, 10, "COMPLETED", "AM_0730", "SELF", "PAID"),
    (date(2026, 9, 8), 2, 7, "COMPLETED", "AM_0900", "ONLINE", "PAID"),
    (date(2026, 8, 20), 1, 11, "COMPLETED", "AM_1030", "SELF", "PAID"),
    (date(2026, 8, 12), 5, 12, "COMPLETED", "AM_0730", "SELF", "GROUP"),
    (date(2026, 8, 2), 1, 7, "COMPLETED", "AM_1030", "ENTRUST", "PAID"),
    (date(2026, 7, 21), 10, 8, "COMPLETED", "AM_0900", "ONLINE", "PAID"),
    (date(2026, 7, 9), 13, 9, "COMPLETED", "AM_0730", "SELF", "PAID"),
    (date(2026, 6, 18), 6, 10, "COMPLETED", "AM_0900", "ONLINE", "PAID"),
    (date(2026, 6, 5), 14, 11, "CANCELLED", "AM_1030", None, "REFUNDED"),
    (date(2026, 5, 22), 16, 12, "COMPLETED", "AM_0730", "SELF", "PAID"),
    (date(2026, 5, 12), 2, 7, "RESCHEDULED", "AM_0900", None, "PAID"),
    (date(2026, 5, 6), 7, 8, "COMPLETED", "AM_0730", "ONLINE", "PAID"),
]
# 历史预约需要对应的历史时段行，否则会被错误挂到未来时段（占位与去重会失真）
hist_slot_start = len(slot_rows)
for a in APPS:
    day, period = a[0], a[4]
    if (day, period) in slot_ids or day >= TODAY:
        continue
    sid += 1
    code, rng, pq, gq, cap = next(x for x in PERIODS if x[0] == period)
    slot_ids[(day, period)] = sid
    slot_rows.append((day, code, rng, pq, gq, pq, gq, cap, "CLOSED", dt(day - timedelta(days=20), 9, 0)))

insert("appointment_slot",
       ["slot_date", "period", "time_range", "personal_quota", "group_quota", "personal_booked",
        "group_booked", "dept_capacity", "status", "create_time"], slot_rows[hist_slot_start:])

STATUS_ORDER = ["PENDING_CONFIRM", "WAIT_PAY", "WAIT_CHECKIN", "CHECKING", "WAIT_REPORT", "WAIT_PICKUP", "COMPLETED"]
PAY_LABEL = {"CASH": "CASH", "WECHAT": "WECHAT", "ALIPAY": "ALIPAY", "CARD": "CARD", "GROUP": "GROUP"}
app_rows = []
app_meta = []           # 供后续表引用
for i, a in enumerate(APPS, 1):
    day, pkg_i, uid, status, period = a[0], a[1], a[2], a[3], a[4]
    pickup, pay = a[5], a[6]
    pkg = PACKAGES[pkg_i - 1]
    rng = dict((p[0], p[1]) for p in PERIODS)[period]
    slot_id = slot_ids[(day, period)]
    app_no = f"AP-{day.strftime('%Y%m%d')}-{i:04d}"
    paid = pay in ("PAID", "GROUP")
    user = users[uid - 1]
    pay_method = "GROUP" if pay == "GROUP" else (["WECHAT", "ALIPAY", "CARD", "CASH"][i % 4] if paid else None)
    report_status = "NONE"
    if status == "WAIT_REPORT":
        report_status = "PENDING"
    elif status == "WAIT_PICKUP":
        report_status = "READY"
    elif status == "COMPLETED":
        report_status = "PICKED"
    ctime = dt(day - timedelta(days=12 + i % 9), 9 + i % 8, (i * 7) % 60)
    checkin = dt(day, 7, 40 + i % 15) if status in ("CHECKING", "WAIT_REPORT", "WAIT_PICKUP", "COMPLETED") else None
    app_rows.append((
        app_no, pkg_i, pkg[1], uid, None, day, slot_id, rng,
        user[2], user[4], (user[5][:8] + "****" + user[5][-4:]) if user[5] else None,
        "需提前完成空腹项目" if i % 5 == 0 else None,
        pkg[6], pkg[6] if paid else 0, "PAID" if paid else ("REFUNDED" if pay == "REFUNDED" else "UNPAID"),
        pay_method, status, checkin, report_status, pickup, None, 3 if status == "WAIT_CHECKIN" else None,
        "超过 30 天未领取，已电话通知" if status == "COMPLETED" and i % 6 == 0 else None,
        1, ctime, dt(day + timedelta(days=1), 10, 20)))
    app_meta.append({"id": i, "day": day, "pkg": pkg_i, "status": status, "pickup": pickup,
                     "user": uid, "paid": paid, "report_status": report_status, "no": app_no})

insert("exam_appointment",
       ["app_no", "package_id", "package_name", "user_id", "batch_id", "booking_date", "slot_id",
        "time_range", "contact_name", "contact_phone", "id_card_digest", "special_need",
        "amount_due", "amount_paid", "pay_status", "pay_method", "status", "checkin_time",
        "report_status", "pickup_method", "crowd_score_id", "owner_id", "remark", "version",
        "create_time", "update_time"], app_rows)

# ---------------------------------------------------------------- 缴费记录
pay_rows = []
for m in app_meta:
    if not m["paid"]:
        continue
    pkg = PACKAGES[m["pkg"] - 1]
    discount = 0 if m["pkg"] % 3 else 20
    pay_rows.append((m["id"], pkg[5], discount, pkg[6] - discount,
                     "GROUP" if m["pickup"] == "GROUP" else ["WECHAT", "ALIPAY", "CARD", "CASH"][m["id"] % 4],
                     f"/uploads/seed/voucher/{((m['id'] - 1) % 4) + 1:02d}.jpg",
                     0, None, 3,
                     dt(m["day"] - timedelta(days=1), 14, 20)))
# 一笔退费
pay_rows.append((29, 260, 0, 260, "WECHAT", "/uploads/seed/voucher/01.jpg", 260, "体检人临时出差，取消预约", 3,
                 dt(date(2026, 6, 4), 11, 5)))
insert("payment_record",
       ["app_id", "receivable", "discount_amount", "received", "pay_method", "voucher_image",
        "refund_amount", "refund_reason", "operator_id", "create_time"], pay_rows)

# ---------------------------------------------------------------- 项目执行进度
prog_rows = []
for m in app_meta:
    if m["status"] not in ("CHECKING", "WAIT_REPORT", "WAIT_PICKUP", "COMPLETED"):
        continue
    items = PACKAGES[m["pkg"] - 1][4]
    done_ratio = {"CHECKING": 0.45, "WAIT_REPORT": 1.0, "WAIT_PICKUP": 1.0, "COMPLETED": 1.0}[m["status"]]
    n_done = int(len(items) * done_ratio)
    for k, iid in enumerate(items):
        if k < n_done:
            st, remark, sup = "DONE", None, None
        elif m["status"] == "CHECKING" and k == n_done:
            st, remark, sup = "CHECKING", None, None
        elif m["status"] == "CHECKING":
            st, remark, sup = "WAITING", None, None
        elif (m["id"] + k) % 17 == 0:
            st, remark, sup = "SUPPLEMENT", "心电图设备故障，改约补检", m["day"] + timedelta(days=7)
        elif (m["id"] + k) % 13 == 0:
            st, remark, sup = "GIVEN_UP", "体检人自愿放弃，已签字确认", None
        else:
            st, remark, sup = "DONE", None, None
        prog_rows.append((m["id"], iid, ITEMS[iid - 1][0], ITEMS[iid - 1][1], st,
                          dt(m["day"], 8, (k * 3) % 60) if st == "DONE" else None,
                          3 if st != "WAITING" else None, remark, sup,
                          dt(m["day"], 8, 0)))
insert("exam_progress",
       ["app_id", "item_id", "item_name", "dept", "status", "check_time", "operator_id",
        "remark", "supplement_date", "create_time"], prog_rows)

# ---------------------------------------------------------------- 报告（多版本）
report_rows = []
report_meta = {}
rid = 0
for m in app_meta:
    if m["status"] not in ("WAIT_PICKUP", "COMPLETED") and not (m["status"] == "WAIT_REPORT" and m["id"] % 2 == 0):
        continue
    pkg = PACKAGES[m["pkg"] - 1]
    issue = m["day"] + timedelta(days=2 + m["id"] % 3)
    if m["status"] in ("WAIT_PICKUP", "COMPLETED"):
        rid += 1
        if m["id"] == 14:  # 该单有 V1 与 V2 两个版本，验证"只看最新有效版"
            report_rows.append((m["id"], 1, f"/uploads/report/rpt-{m['id']:03d}-v1.pdf",
                                f"{pkg[1]}-报告-v1.pdf", issue - timedelta(days=1), "李国强", 0, "ARCHIVED", 3,
                                dt(issue - timedelta(days=1), 16, 30)))
            report_rows.append((m["id"], 2, f"/uploads/report/rpt-{m['id']:03d}-v2.pdf",
                                f"{pkg[1]}-报告-v2.pdf", issue, "李国强", 1, "ARCHIVED", 3,
                                dt(issue, 10, 15)))
        else:
            report_rows.append((m["id"], 1, f"/uploads/report/rpt-{m['id']:03d}-v1.pdf",
                                f"{pkg[1]}-报告.pdf", issue, "李国强", 1, "ARCHIVED", 3,
                                dt(issue, 10, 15)))
        report_meta[m["id"]] = rid if m["id"] != 14 else rid + 1
insert("exam_report",
       ["app_id", "version_no", "file_path", "file_name", "issue_date", "doctor_name",
        "is_latest", "status", "uploader_id", "create_time"], report_rows)

# ---------------------------------------------------------------- 领取记录
pickup_rows = []
for m in app_meta:
    if m["status"] != "COMPLETED":
        continue
    pt = m["pickup"] or "SELF"
    ptime = m["day"] + timedelta(days=4)
    picker = m["no"]
    name = users[m["user"] - 1][2]
    if pt == "ENTRUST":
        picker_name, relation, auth = "刘建国", "配偶", f"/uploads/seed/auth/{((m['id']) % 4) + 1:02d}.jpg"
    else:
        picker_name, relation, auth = name, "本人", None
    pickup_rows.append((m["id"], report_meta.get(m["id"], 1), pt, picker_name,
                        "310101********" + f"{1000 + m['id']}"[-4:], users[m["user"] - 1][4],
                        relation, auth, dt(ptime, 15, 10), 3,
                        "核对身份证原件并留存复印件" if pt == "ENTRUST" else None,
                        dt(ptime, 15, 10)))
insert("report_pickup",
       ["app_id", "report_id", "pickup_type", "picker_name", "picker_id_card", "picker_phone",
        "relation", "auth_file", "pickup_time", "operator_id", "remark", "create_time"], pickup_rows)

# ---------------------------------------------------------------- 拥挤度规则（3 版本）
W3 = {"deptCapacity": 22, "itemMix": 18, "slotLoad": 20, "duration": 14, "arrivalRate": 16, "deviceDown": 10}
W2 = {"deptCapacity": 25, "itemMix": 15, "slotLoad": 25, "duration": 12, "arrivalRate": 15, "deviceDown": 8}
W1 = {"deptCapacity": 30, "itemMix": 15, "slotLoad": 25, "duration": 10, "arrivalRate": 15, "deviceDown": 5}
TH = {"recommend": 40, "normal": 70}
rule_rows = [
    (1, json.dumps(W1, ensure_ascii=False), json.dumps(TH, ensure_ascii=False), "HISTORY", dt(date(2026, 4, 1), 0, 0), 2, "系统初始化默认权重"),
    (2, json.dumps(W2, ensure_ascii=False), json.dumps(TH, ensure_ascii=False), "HISTORY", dt(date(2026, 7, 1), 0, 0), 2, "提高时段预约量权重"),
    (3, json.dumps(W3, ensure_ascii=False), json.dumps(TH, ensure_ascii=False), "ACTIVE", dt(date(2026, 10, 1), 0, 0), 2, "纳入设备停用惩罚项"),
]
insert("crowd_rule", ["version_no", "weights_json", "thresholds_json", "status", "effective_time",
                      "create_by", "remark", "create_time"],
       [(r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[4]) for r in rule_rows])

# ---------------------------------------------------------------- 拥挤度计算留痕（34 条）
def level_of(total):
    return "RECOMMEND" if total <= 40 else ("NORMAL" if total <= 70 else "NOT_RECOMMEND")

score_rows = []
for k in range(34):
    app_i = (k % 32) + 1
    m = app_meta[app_i - 1]
    pkg = PACKAGES[m["pkg"] - 1]
    slot_id = slot_ids.get((m["day"], "AM_0730")) or slot_ids[(TODAY + timedelta(days=2), "AM_0730")]
    dept = min(1.0, 0.30 + (k * 13 % 60) / 100)
    mix = min(1.0, 0.35 + (len(pkg[4]) / 28) * 0.5)
    load = min(1.0, 0.30 + (k * 17 % 65) / 100)
    dur = min(1.0, pkg[7] / 180)
    arr = min(1.0, 0.62 + (k * 11 % 30) / 100)
    dev = 0.0 if k % 9 else 0.75
    vals = [dept, mix, load, dur, arr, dev]
    wts = [W3[x] / 100 for x in ["deptCapacity", "itemMix", "slotLoad", "duration", "arrivalRate", "deviceDown"]]
    total = round(sum(v * 100 * w for v, w in zip(vals, wts)), 1)
    lv = level_of(total)
    ver = 3 if k < 26 else 2
    snapshot = {"ruleVersion": f"v{ver}", "slotId": slot_id, "appointmentId": app_i,
                "inputs": {x: round(v, 2) for x, v in zip(["deptCapacity", "itemMix", "slotLoad", "duration", "arrivalRate", "deviceDown"], vals)},
                "weights": W3, "total": total, "level": lv,
                "generatedAt": dt(m["day"] - timedelta(days=10), 9, (k * 7) % 60)}
    detail = {"scores": [round(v * 100, 1) for v in vals],
              "contributions": [round(v * 100 * w, 2) for v, w in zip(vals, wts)]}
    factors = f"历史到检率 {int(arr*100)}（权重 16）、时段预约量 {int(load*100)}（权重 20）"
    unmet = "无" if lv != "NOT_RECOMMEND" else "名额剩余不足 5 个"
    tip = "建议提前 10 分钟到检以错峰" if lv != "RECOMMEND" else "该时段负载较低，可正常预约"
    adj_s = adj_r = adj_b = adj_t = None
    if k in (5, 12, 21):
        adj_s = round(max(10, total - 12), 1)
        adj_r = "体检人指定上午时段，结合现场排队情况下调拥挤度"
        adj_b = 2
        adj_t = dt(m["day"] - timedelta(days=9), 11, 0)
    score_rows.append((f"CS-{m['day'].strftime('%Y%m%d')}-{k + 1:04d}", app_i, slot_id,
                       ver, ver, json.dumps(snapshot, ensure_ascii=False), json.dumps(detail, ensure_ascii=False),
                       total, lv, factors, unmet, tip, adj_s, adj_r, adj_b, adj_t,
                       dt(m["day"] - timedelta(days=10), 9, (k * 7) % 60)))
insert("crowd_score",
       ["score_no", "app_id", "slot_id", "rule_id", "rule_version", "input_snapshot", "detail_json",
        "total_score", "level", "main_factors", "unmet_conditions", "improve_tip",
        "adjusted_score", "adjust_reason", "adjust_by", "adjust_time", "create_time"], score_rows)

# ---------------------------------------------------------------- 消息
msgs = []
for i, m in enumerate(app_meta[:12], 1):
    title = {"WAIT_PAY": "预约已审核，请完成缴费", "WAIT_CHECKIN": "体检预约成功，请按时到检",
             "WAIT_REPORT": "体检已完成，报告正在出具", "WAIT_PICKUP": "您的体检报告已可领取",
             "COMPLETED": "报告已领取，祝您健康"}.get(m["status"], "预约状态更新")
    msgs.append((m["user"], title, f"预约编号 {m['no']}，套餐 {PACKAGES[m['pkg']-1][1]}。", m["no"],
                 "APPOINTMENT", 0 if i > 4 else 1, dt(m["day"] - timedelta(days=8), 9, 30)))
msgs.append((3, "待处理：3 条预约等待审核", "请在前台工作台处理待审核预约。", None, "SYSTEM", 0, dt(TODAY, 8, 40)))
msgs.append((3, "报告下载审计提示", "用户张伟下载了报告 AP-20261020-0014（第二次身份校验通过）。", "AP-20261020-0014", "REPORT", 1, dt(date(2026, 10, 23), 14, 12)))
insert("sys_message", ["user_id", "title", "content", "biz_no", "biz_type", "read_flag", "create_time"], msgs)

# ---------------------------------------------------------------- 操作日志
logs = []
acts = [("预约管理", "审核通过"), ("预约管理", "登记收费"), ("到检执行台", "核验签到"),
        ("报告归档", "上传报告"), ("报告领取", "委托代领登记"), ("拥挤度规则", "保存新版本"),
        ("套餐管理", "编辑套餐"), ("报告领取", "报告授权下载")]
for i in range(24):
    a = acts[i % len(acts)]
    logs.append((3 if a[0] != "拥挤度规则" else 2, "front01" if a[0] != "拥挤度规则" else "mgr01",
                 a[0], a[1], app_meta[i % 32]["no"], f"{a[1]}成功，状态已同步",
                 "127.0.0.1", dt(TODAY - timedelta(days=i, hours=i % 8), 9 + i % 9, (i * 13) % 60)))
insert("operation_log", ["user_id", "username", "module", "action", "biz_no", "detail", "ip", "create_time"], logs)

out = SCHEMA + "\n\n-- ===================== 测试数据 =====================\n\n" + "\n".join(A)
(HERE / "init.sql").write_text(out, encoding="utf-8")
print("init.sql 生成完成，行数:", len(out.splitlines()))
