/* global cm */

var status, str, select, list, error = false, point = 0;

function start() {
	status = -1;
	str = "";
	select = -1;
	str += "\r\n#L1#貓頭鷹查詢#l";
	cm.sendSimple("您好，我是GM派來的超智慧NPC!!\r\n我能幫助您搜尋道具來查找是否有玩家在販售.." + str);
}

function action(mode, type, selection) {
	if (mode == 1) {
		status++;
	} else {
		status--;
		cm.dispose();
		return;
	}
	switch (status) {
	case 0:
		str = selection;
		cm.sendGetText("請輸入想查詢道具:");
		break;
	case 1:
		cm.sendOk(cm.searchData(str, cm.getText()));
		break;
	case 2:
		if (!cm.foundData(str, cm.getText())) {
			cm.dispose();
			return;
		}
		if (select == -1) {
			select = selection;
			if (cm.getText() != "") {
				if (!cm.haveItem(5230000)) {
					cm.sendSimple("看起來沒有智慧貓頭鷹呢，沒關係我這裡有兩個選擇!!\r\n#b#L1#使用5點GASH#l\r\n#L2#使用5點楓葉點數#l");
				} else {
					cm.sendYesNo("確定要搜尋此物品嗎??");
				}
			} else {
				cm.dispose();
				return;
			}
		}
		break;
	case 3:
		point = selection;
		switch (str) {
		case 1:
			cm.OwlAdv(point, select);
			break;
		default:
			cm.dispose();
		}
	}
}
