var status = 0;
var selectedItem = -1;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == 1)
		status++;
	else
		cm.dispose();
	if (status == 0 && mode == 1) {
		if (cm.getQuestStatus(3034) == 2) {
			var selStr = "你一直在幫助我，讓我很感動!!!\r\n如果有任何的黑暗水晶母礦我可以用一個 #b500000 楓幣#k 的價格幫您製作."

				cm.sendYesNo(selStr);
		} else {
			cm.sendOk("如果沒有什麼事情的話，請走開別煩我!.");
			cm.dispose();
		}
	} else if (status == 1 && mode == 1) {
		cm.sendGetNumber("好您想要製作多少個??", 1, 1, 100);
	} else if (status == 2 && mode == 1) {
		var complete = true;
		var itemID = 4005004;
		var matID = 4004004;
		var matQty = 10;
		var cost = 500000;

		if (cm.getMeso() < cost * selection) {
			cm.sendOk("很抱歉我不能為您做免費的!")
		} else {
			if (!cm.haveItem(matID, matQty * selection)) {
				complete = false;
			}
		}

		if (!complete)
			cm.sendOk("我需要母礦來製作..");
		else {
			cm.gainItem(matID, -matQty * selection);
			cm.gainMeso(-cost * selection);
			cm.gainItem(itemID, selection);
			cm.sendOk("製作完畢!");
		}
		cm.dispose();
	}
}
