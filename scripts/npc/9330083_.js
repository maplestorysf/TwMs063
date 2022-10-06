/*
 By 梓條
 */

var status = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 0) {
		var Editing = false //true=顯示;false=開始活動
          if(Editing){
          cm.sendOk("暫停運作");
          cm.dispose();
          return;
        } 
			cm.sendSimple("#b歡迎玩家 #r#h ##k 兌換#r綿羊單人床#i3010054#" +
            "#k\r\n#L101##r黃金豬#i4032226##bx1000#r換#b綿羊單人床 #i3010054#\r\n");
        } else if (status == 1) {
            
            if (selection == 101) {
                if (cm.haveItem(4032226, 1000) ) {
                    cm.gainItem(4032226, -1000);
                    cm.gainItem(3010054, 1);
                    cm.sendOk("獲得#i3010054#");
                    cm.dispose();
                } else {
                    cm.sendOk("您身上沒有足夠的#i4032226#,請在次確認");
                    cm.dispose();
                }
            }
        }
    }
}

	