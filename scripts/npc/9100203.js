var status = -1;

function start() {
	cm.sendYesNo("想要開始打小鋼珠嗎?");
}

function action(mode, type, selection) {
	if (mode == -1)
		cm.dispose();
	else {
		if (mode == 1)
			status++;
		else {
			cm.sendNext("等到想到賭博的話再來找我。");
			cm.dispose();
			return;
		}
		if (status == 0) {
			cm.開啟小鋼珠();
			cm.dispose();
		}
	}
}
