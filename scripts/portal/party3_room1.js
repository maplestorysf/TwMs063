﻿function enter(pi) {
	if (pi.getPlayer().getParty() != null && pi.isLeader()) {
		pi.warpParty(920010200);
		pi.playPortalSE();
	} else {
		pi.playerMessage(5,"請隊長進入洞口。");
	}
}