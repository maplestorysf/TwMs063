/* Kedrick
Fishking King NPC
 */

var status = -1;
var sel;

function action(mode, type, selection) {
	if (mode == 1) {
		status++;
	} else {
		if (status == 0) {
			cm.dispose();
			return;
		}
		status--;
	}

	if (status == 0) {
		cm.sendSimple("我能為您做什麼嗎？？\n\r #L4#教我怎麼釣魚。#l \n\r #L5#使用500個鮭魚換取 #i1142071#標準國字勳章 [期限 : 30 天]#l\n\r #L6#購買高級釣魚VIP椅子#l");
	} else if (status == 1) {
		sel = selection;
		if (sel == 4) {
			cm.sendOk("2017/01/26釣魚更新說明:\r\n#b普通釣魚:#k\r\n所需的物品有...\r\n#r普通釣魚竿x1#k、#r普通魚餌一組#k、#r藍色木椅x1\r\n#b高級釣魚:#k\r\n所需的物品有...\r\n#r高級釣魚竿x1#k、#r高級魚餌一組#k、#r貴族椅子x1(找我購買)\r\n#g釣魚收穫時間:\r\n#b普通釣魚:每6分鐘收穫一次\r\n高級釣魚:每4分鐘收穫一次");
			cm.safeDispose();
		} else if (sel == 5) {
			if (cm.haveItem(4031648, 500)) {
				if (cm.canHold(1142071)) {
					cm.gainItem(4031648, -500);
					cm.gainItemPeriod(1142071, 1, 30);
					cm.sendOk("恭喜拿到了 #b#i1142071##k!")
				} else {
					cm.sendOk("請確認裝備欄是否有足夠。");
				}
			} else {
				cm.sendOk("請給我 500個 #i4031648:# 我才能給你 #i1142071#");
			}
			cm.safeDispose();
		} else if (sel == 6) {
			cm.sendNext("具備高級釣魚VIP的資料:\r\n#r楓幣: 10,000,000#k、#r#i5340001# x1 #k、#r#i2300001# x1組");
		}
	} else if (status == 2) {
		if (sel++) {
			if (cm.haveItem(5340001, 1) && cm.haveItem(2300001, 1)) {
				if (cm.getMeso() >= 10000000) {
					if (cm.canHold(3010060)) {
						cm.gainMeso(-10000000);
						cm.gainItem(3010060, 1);
						cm.sendOk("恭喜您有高級釣魚VIP的資格! 獲得了:#i3010060##z3010060# 開始好好享受高級釣魚VIP的樂趣吧!");
					} else {
						cm.sendOk("請確認裝飾欄是否有足夠。");
					}
				} else {
					cm.sendOk("貌似楓幣不足.....");
				}
			} else {
				cm.sendOk("您確定您已經具備了高級釣魚VIP的資格了嗎??");
			}
		}
		cm.safeDispose();
	}
}
