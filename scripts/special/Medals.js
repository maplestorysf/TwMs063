var status = -1;
var debug = true;

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === 1) {
        status++;
    } else if (mode === 0) {
        status--;
    } else {
        cm.dispose();
        return;
    }
	/*if (debug && !cm.getPlayer().isGM()){
		cm.dispose();
        return;
	}*/
    if (status == 0) {
        cm.sendYesNo("請問您是否要領取RC勳章？");
    } else if (status == 1) {
        if (!cm.ReceiveMedal()) {
            cm.sendNext("請檢查您的背包是否已滿、本帳號已經領取過該勳章或是目前不在領取勳章清單內");
        } else {
            cm.sendNext("勳章已經發到您的背包裡了，請查收。");
        }
        cm.dispose();
    } else {
        cm.dispose();
    }
}