function start() {
	if (cm.getQuestStatus(8510) == 2) {
    cm.warp(701010321);
    cm.dispose();
	} else {
	    cm.sendOk("你沒有完成農民的拜託任務!");
    cm.dispose();
}
}