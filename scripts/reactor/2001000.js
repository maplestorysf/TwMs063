/* 
女神的遺跡組隊任務
修復女神的食人花出現位置固定問題
by:Kodan
*/

function act() {
	var ran = Math.floor((Math.random() * 6) + 1);
	var em = rm.getEventManager("OrbisPQ");
	if (em != null) {
		if (em.getProperty("stage7").equals("1") && ran >= 4) {
			rm.spawnMonster(9300049);
			rm.mapMessage(6, "女神的食人花出現了!");
			em.setProperty("stage7", "2");
		} else if (em.getProperty("stage8").equals("7") && em.getProperty("stage7").equals("1")) {
			rm.spawnMonster(9300049);
			rm.mapMessage(6, "女神的食人花出現了!");
			em.setProperty("stage8", "8");
		} else {
			rm.spawnMonster(9300048);
			em.setProperty("stage8",  parseInt(em.getProperty("stage8")) + 1);
			rm.mapMessage(6, em.getProperty("stage8"));
		}
	}
}
