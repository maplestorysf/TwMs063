/*
	鬧鬼宅邸 觸發-美伊德 (229010000)
**/

function start() {

    if (cm.getParty() == null) { // No Party
	cm.sendOk("請組隊再來找我");
    } else if (!cm.isLeader()) { // Not Party Leader
	cm.sendOk("請叫你的隊長來找我!");
    } else {
	// Check if all party members are within Levels 50-200
	var party = cm.getParty().getMembers();
	var mapId = cm.getMapId();
	var next = true;
	var levelValid = 0;
	var inMap = 0;

	var it = party.iterator();
	while (it.hasNext()) {
	    var cPlayer = it.next();
	    if ((cPlayer.getLevel() >= 50 && cPlayer.getLevel() <= 200) || cPlayer.getJobId() == 900) {
		levelValid += 1;
	    } else {
		next = false;
	    }
	    if (cPlayer.getMapid() == mapId) {
		inMap += (cPlayer.getJobId() == 900 ? 4 : 1);
	    }
	}
	if (party.size() > 1 || inMap < 1) {
	    next = false;
	}
	if (next) {
	    var em = cm.getEventManager("QiajiPQ");
	    if (em == null) {
			cm.sendOk("找不到腳本，請聯繫GM！");
			cm.dispose();
			return;		
	    } else {
		var prop = em.getProperty("state");
		if (prop == null || prop.equals("0")) {
		    em.startInstance(cm.getParty(),cm.getMap());
		} else {
		    cm.sendOk("已經有隊伍在裡面挑戰了。");
			cm.dispose();
			return;
			}
	    }
	} else {
	    cm.sendOk("你的隊伍需要一個人,等級必須在50-200之間,請確認你的隊友有沒有都在這裡,或是裡面已經有人了!");
		cm.dispose();
		return;
	}
    }
    cm.dispose();
}

function action(mode, type, selection) {
}