var status = 0;
var qChars = new Array("Q1: 下列哪種type轉過去資料會丟失??#Float To Double#Dec to Hex#Int to Char#Hex to Dec#3",
    "Q1: CDC character set contains 64 characters, To represent any possible 10 character string needs at least?#55Bits#60Bits#65Bits#70Bits#2",
    "Q1: Which of the following is an imperative programming language?#Ada#IDE#Java#NET#1",
    "Q1: (In C) int x=1; if (x=2) x=3; the value of x is ____.#1#0#2#3#4",
    "Q1: 在Java語言中 int i=100; x >> 8 那麼x的值是多少??#1#0#3#3#2");
var qItems = new Array("Q2: OSI七層協定中Tcp是屬於第幾層?#第三層#第四層#第一層#第五層#2",
    "Q2: 在Java語言中 (for int i=0; i<=100; i++) 那麼i的值是多少? #0#101#99#100#4",
    "Q2: 場效電晶體工作於飽和區，電路具有何種功能?#倍壓#輸出定電流#電流放大#開關控制#2",
    "Q2: 一個全波橋式整流電路輸入之交流正弦波電壓為16Vp-p，則輸出之平均電壓約為多少?#5.1V#8.2V#7.2V#9.4V#1",
    "Q2: 下列元件編號中哪個是運算放大器?#2sk30#2SC1815#ne555#uA741#4");
var qMobs = new Array("Q3: 下列何者屬於非接觸式IC卡?#健保卡#悠遊卡#提款卡#自然人憑證卡#2",
    "Q3: 下列何者是用於一般個人電腦網路線接頭之規格?#RJ44#RJ45#RJ11#RJ22#2",
    "Q3: 在Class B網段中 下列總共幾ip是正確的?#65536#16777216#256#127#1",
    "Q3: 在Class C網段中 下列總共幾ip是正確的?#65536#16777216#256#127#3",
    "Q3: 在Class A網段中 下列總共幾ip是正確的?#65536#16777216#256#127#2",
    "Q3: 在近來最流行的RSA加密病毒，請問它是屬於何種加密#非對稱加密#對稱加密#DEC#私人加密#1",
    "Q3: 0111正確答案為?#4#5#6#7#4");
var qQuests = new Array("Q4: 11001000011101100011 屬於下列哪種?#C8763#C8673#C8637#C8736#1",
    "Q4: Google Derive是使用何種語言編寫#C++#Java#PHP#Python#4",
    "Q4: 請看以下程式碼求出f的答案def f(x,l=[]):\r\nfor i in range(x):\r\nl.append(i*i)\r\nprint l#3#2#23#32#1",
    "Q4: 下列何者是WWW的通訊協定?#https#http#ftp#title#2",
    "Q4: 每一部主機在Internet都有一個識別數字型代號稱之為?#url#DNS#TCP#IP#4",
    "Q4: 下列何者是屬於Class C的IP網址?#192.83.166.5#258.128.33.24#120.80.40.20#140.92.1.50#1");
var qTowns = new Array("Q5: 一般FAX是指?#電傳視訊系統#電子遊戲系統#電傳會議#電傳文件系統#4",
    "Q5: 分散式處理系統簡稱?#MNPS#BBPS#DDPS#RNPS#3",
    "Q5: 捷運上下車的順序，資料結構觀之?#串列結構#佇列結構#堆疊結構#樹狀結構#1",
    "Q5: 以泡沫排序搜尋某資料最多要找幾次才能從1000比資料找到要的資料?#1#10#100#5#3",
    "Q5: 在Java語言中 int i=100; x >> 100 那麼x的值是多少??#6#5#4#3#1",
    "Q5: 在Java語言中 int i=100; i >> 100 ^ 50 % 30 / 50 ^ 2 << 5 那麼x的值是多少??#60#70#80#50#2");
var correctAnswer = 0;

function start() {
    if (cm.haveItem(4031058, 1)) {
        cm.sendOk("#h #,你已經有了 #t4031058# 不要讓廢我時間.");
        cm.dispose();
    }
    if (!(cm.haveItem(4031058, 1))) {
        cm.sendNext("歡迎光臨 #h #, 我是 #p2030006#.\r\n看來你已經走了很遠到達了這個階段.");
    }
}

function action(mode, type, selection) {
    if (mode == -1)
        cm.dispose();
    else {
        if (mode == 0) {
            cm.sendOk("下次再見.");
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 1)
            cm.sendNextPrev("#h #, 如果你給我 #b黑暗水晶#k 我將會讓你試著回答5個問題,若您5個問題都答對您將得到 #v4031058# #b智慧項鍊#k.");
        else if (status == 2) {
            if (!cm.haveItem(4005004)) {
                cm.sendOk("#h #, 你沒有 #b黑暗水晶#k");
                cm.dispose();
            } else {
                cm.gainItem(4005004, -1);
                cm.sendSimple("測驗開始 #b接受挑戰吧!#k.\r\n\r\n" + getQuestion(qChars[Math.floor(Math.random() * qChars.length)]));
                status = 2;
            }
        } else if (status == 3) {
            if (selection == correctAnswer)
                cm.sendOk("#h # 你答對了.\n準備答下一題??");
            else {
                cm.sendOk("你答錯了的答案!.\r\n很抱歉你必須在給我一個 #b黑暗水晶#k 才可以再挑戰!");
                cm.dispose();
            }
        } else if (status == 4)
            cm.sendSimple("測驗開始 #b接受挑戰吧!#k.\r\n\r\n" + getQuestion(qItems[Math.floor(Math.random() * qItems.length)]));
        else if (status == 5) {
            if (selection == correctAnswer)
                cm.sendOk("#h # 你答對了.\n準備答下一題??");
            else {
                cm.sendOk("你答錯了的答案!.\r\n很抱歉你必須在給我一個 #b黑暗水晶#k 才可以再挑戰!");
                cm.dispose();
            }
        } else if (status == 6) {
            cm.sendSimple("測驗開始 #b接受挑戰吧!#k.\r\n\r\n" + getQuestion(qMobs[Math.floor(Math.random() * qMobs.length)]));
            status = 6;
        } else if (status == 7) {
            if (selection == correctAnswer)
                cm.sendOk("#h # 你答對了.\n準備答下一題??");
            else {
                cm.sendOk("你答錯了的答案!.\r\n很抱歉你必須在給我一個 #b黑暗水晶#k 才可以再挑戰!");
                cm.dispose();
            }
        } else if (status == 8)
            cm.sendSimple("測驗開始 #b接受挑戰吧!#k.\r\n\r\n" + getQuestion(qQuests[Math.floor(Math.random() * qQuests.length)]));
        else if (status == 9) {
            if (selection == correctAnswer) {
                cm.sendOk("#h # 你答對了.\n準備答下一題??");
                status = 9;
            } else {
                cm.sendOk("你答錯了的答案!.\r\n很抱歉你必須在給我一個 #b黑暗水晶#k 才可以再挑戰!");
                cm.dispose();
            }
        } else if (status == 10) {
            cm.sendSimple("最後一個問題.\r\n測驗開始 #b接受挑戰吧!#k.\r\n\r\n" + getQuestion(qTowns[Math.floor(Math.random() * qTowns.length)]));
            status = 10;
        } else if (status == 11) {
            if (selection == correctAnswer) {
                cm.gainItem(4031058, 1);
                cm.warp(211000001, 0);
                cm.sendOk("恭喜 #h #, 你太強大了.\r\n拿著這個 #v4031058# 去找你的轉職教官吧!.");
                cm.dispose();
            } else {
                cm.sendOk("太可惜了,差一題就可以通關了!! 多多加油><.\r\n很抱歉你必須在給我一個 #b黑暗水晶#k 才可以再挑戰!");
                cm.dispose();
            }
        }
    }
}

function getQuestion(qSet) {
    var q = qSet.split("#");
    var qLine = q[0] + "\r\n\r\n#L0#" + q[1] + "#l\r\n#L1#" + q[2] + "#l\r\n#L2#" + q[3] + "#l\r\n#L3#" + q[4] + "#l";
    correctAnswer = parseInt(q[5], 10);
    correctAnswer--;
    return qLine;
}