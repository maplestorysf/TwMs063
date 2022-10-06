function enter(pi) {
    if (pi.getJob() >= 1000) {
	if (pi.haveItem(4032179)) { // Search warrent
	    pi.playerMessage("開始搜索....");
	}
	pi.playPortalSE();
	pi.warp(130010000, 3);
    } else {
	pi.playerMessage("只有皇家騎士團成員才能進入...");
    }
}