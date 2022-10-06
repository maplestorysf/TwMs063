var status = -1;
var id;
var links = Array("123456", "321", "123", "12333");
var newitem;
var bean;
var day;
var haha;

function start() {
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == 1) {
		status++;
	} else {
		status--;
	}
	if (status == 0) {
		cm.sendSimple("親愛的#h \r\n\r\n您好我是#p9120104#看起來您對小鋼珠系統也有點興趣呢！\r\n有什麼我可以服務的嗎??\r\n#L0##r小鋼珠轉蛋#k#l\r\n#L1##b獎品兌換#k#l\r\n#L2#只專屬你的服務！#l");
	} else if (status == 1) {
		if (selection == 0) {
			cm.sendNext("轉蛋尚未開放。");
			cm.dispose();
		} else if (selection == 1) {
			haha = selection;
			cm.sendSimple("主人今天想要換什麼道具??\r\n#L0##i1702176#鬼焰刀30天");
		} else if (selection == 2) {
			id = Math.floor(Math.random() * links.length);
			cm.sendNext("今天我推薦你的網址是:" + links[id]);
			cm.dispose();
		}

	} else if (status == 2) {
		if (haha = 1) {
			newitem = 1702176;
			bean = 250;
			day = 30;
		}
		cm.sendYesNo("您確定要兌換一個 #b#t" + newitem + "##k? \r\n以下是你所需要的材料。\r\n\小鋼珠 x" + bean + "#k");
	} else if (status == 3) {
		if (cm.getBeans() < bean) {
			cm.sendOk("很抱歉，由於您的材料不足所以無法幫您製作，假如需要的話可以再來找我談談。");
		} else {
			if (cm.canHold(newitem)) {
				cm.gainBeans(-bean);
				cm.gainItem(newitem, 1, day);
				cm.sendOk("好了我已經幫您做好了，還需要的話再來找我談~");
			} else {
				cm.sendOk("看來您還是少了我所需要的材料請檢查是否都到齊了！");
			}
		}
		cm.dispose();
	}
}
