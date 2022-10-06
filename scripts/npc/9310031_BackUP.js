// 身上裝備卷軸次數擴充 by:Kodan
/* 如果不會修改請勿更動 */
load('nashorn:mozilla_compat.js');
importPackage(Packages.server);

var status = -1;
var slot = Array();
var selected;
var error = false;

/* 客製化設置區 */
var item = 4001126; //所需道具
var quantity = 100; //所需道具數量
var maxtimes = 15; //限制最大使用數


function start() {
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == 0 && status == 1 || mode == 0 && status == 0 || mode == 0 && status == 2) {
		cm.sendOk("需要的話再來找我吧。");
		cm.dispose();
		return;
	}

	if (mode == 1)
		status++;
	else
		status--;

	if (status == 0) {
		cm.sendNext("您好我是大G派來的使者\r\n您如果給我#i" + item + "##t" + item + "#" + quantity + "片的話\r\n我就能幫你提升一次裝備卷軸次數！");
	} else if (status == 1) {
		var avail = "";
		for (var i = -1; i > -199; i--) {
			if (cm.getInventory(-1).getItem(i) != null) {
				var itemId = cm.getInventory(-1).getItem(i).getItemId();
				if (itemId == null) {
					i--; //防止下一步錯誤
				}
				if (cm.isCash(itemId)) {
					continue;
				}
			}
			if (cm.getInventory(-1).getItem(i) != null) {
				avail += "#L" + Math.abs(i) + "##t" + cm.getInventory(-1).getItem(i).getItemId() + "##l\r\n";
			}
			slot.push(i);
		}
		cm.sendSimple("要升級哪一件裝備呢??\r\n#b" + avail);
	} else if (status == 2) {
		selected = selection - 1;
		cm.sendYesNo("你想要修改你的 #b#t" + cm.getInventory(-1).getItem(slot[selected]).getItemId() + "##k 卷軸數量??");
	} else if (status == 3) {
		var name = MapleItemInformationProvider.getInstance().getName(cm.getInventory(-1).getItem(slot[selected]).getItemId());
		var ccc = cm.getInventory(-1).getItem(slot[selected]).getUpgradeSlots() + 1;
		var checkccc = ccc + cm.getInventory(-1).getItem(slot[selected]).getLevel();
		if (checkccc > maxtimes) {
			cm.sendNext("貌似已經到" + maxtimes + "次的達上限了阿!!!");
			cm.dispose();
			return;
		}
		if (cm.haveItem(item, quantity)) {
			cm.gainItem(item, -quantity);
			cm.changeStat(slot[selected], 15, ccc);
			cm.getPlayer().reloadC();
			cm.sendNext("你的 #b#t" + cm.getInventory(-1).getItem(slot[selected]).getItemId() + "##k 卷軸次數已成功升級為 " + ccc + ".");
			cm.worldMessage(6, "[裝備升級系統] : 恭喜[" + cm.getPlayer().getName() + "] 成功將" + name + "卷軸次數已成功升級為" + checkccc + "次!");
		} else {
			cm.sendNext("如果給我#i" + item + "##t" + item + "#" + quantity + "片的話那就太好了!!");
		}
		cm.dispose();
	}
}
