package in.dragonbra.javasteam.steam.handlers.steamfriends.callback;

import in.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientPersonaState;
import in.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState;
import in.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg;

import java.util.LinkedList;
import java.util.List;

/**
 * This callback is fired in response to someone changing their friend details over the network.
 */
public class PersonaStatesCallback extends CallbackMsg {

    private List<PersonaState> personaStates = new LinkedList<>();

    public PersonaStatesCallback(CMsgClientPersonaState.Builder body) {
        for (CMsgClientPersonaState.Friend friend : body.getFriendsList()) {
            personaStates.add(new PersonaState(friend));
        }
    }

    public List<PersonaState> getPersonaStates() {
        return personaStates;
    }
}
