function start() {
	cm.sendYesNo("你確定要離開這個地圖嘛？？");
}

function action(mode, type, selection) {
	if (mode == 1) {
		cm.warpMap(105100100, 0);
		cm.channelMapPlayerAllBuuff();
	}
	cm.dispose();
}
