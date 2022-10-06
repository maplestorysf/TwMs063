var status = -1;
var meso = 0;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (mode == 0) {
			cm.getPlayer().setHp(0);
			cm.getPlayer().updateSingleStat(Packages.client.MapleStat.HP, 0);
			cm.sendOk("休想無視我說的話 !\r\n(結果你被醫師害死了 ...)");
			cm.dispose();
			return;
		}
		if (mode == 1) {
			status++;
		} else {
			status--;
		}
		if (status == 0) {
			cm.sendSimple("哎呀 經過我嚴密的檢查 你得了不治之症\r\n你可以選擇付 10 楓幣渺小的手術費我可能會幫你醫治好\r\n請問你是否要付手術費醫治你的疾病 ?\r\n#L0##b好 請幫我醫治吧 !\r\n#L1#不 這手術費根本坑錢啊 !");
		} else if (status == 1) {
			if (selection == 0) {
				meso = cm.getMeso();
				if (cm.getMeso() >= 10) {
					cm.gainMeso(-10);
					cm.getPlayer().setHp(0);
					cm.getPlayer().updateSingleStat(Packages.client.MapleStat.HP, 0);
					cm.sendOk("很不幸地 治療失敗 我已經盡力了 ...\r\n(結果你還是死了 ...)");
					cm.dispose();
				} else {
					cm.gainMeso(-meso);
					cm.getPlayer().setHp(0);
					cm.getPlayer().updateSingleStat(Packages.client.MapleStat.HP, 0);
					cm.sendOk("雖然你錢不夠 我們還是很仁慈的接受\r\n但很不幸地 手術還是失敗了\r\n(結果你還是死了 ...)");
					cm.dispose();
				}
			} else {
				cm.getPlayer().setHp(0);
				cm.getPlayer().updateSingleStat(Packages.client.MapleStat.HP, 0);
				cm.sendOk("你怎麼可以放棄治療呢 ?\r\n(結果你死了 ...)");
				cm.dispose();
			}
		}
	}
}
