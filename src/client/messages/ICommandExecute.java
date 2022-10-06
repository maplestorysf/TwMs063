package client.messages;

import client.MapleClient;

public interface ICommandExecute {

    boolean execute(MapleClient c, String[] splitted);

    String getHelp();
}
