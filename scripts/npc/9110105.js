var status = -1

function start() {
	cm.sendNext("哇! 您是誰?!");
}

function action(mode, type, selection) {
	if (mode == 1) {
		status++
	} else {
		if (status == 0) {
			cm.sendOk("前面是一條看起來很詭異的道路，一個永無止盡的地方。如果我是您我會轉身回頭。");
		}
		cm.dispose();
		return;
	}
	if (status == 0) {
		cm.sendYesNo("什麼!? 您想繼續前進嗎?");
	} else if (status == 1) {
		cm.sendNext("...好的，那我就成全你的意思了!")
	} else if (status == 2) {
		cm.warp(800040300, 0);
		cm.dispose();
	}
}
