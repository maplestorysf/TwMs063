var status = 0;
var serverName = "TWMS";

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    mode == 1 ? status++ : status--;
	
    switch (status) {
        case 0:
            if (cm.getChar().getMapId() == 0) {
                cm.sendNext("歡迎來到 " + serverName + " !");
            } else {
                cm.sendOk("歡迎來到 " + serverName + " !");
            }
            break;
        case 1:
            if (cm.getChar().getMapId() == 0) {
                cm.warp(1, 0);
            }
            cm.dispose();
            break;
        default:
            cm.dispose();
            break;
    }
}