package org.jboss.as.console.client.core.message;

import java.util.List;

/**
 * How about using the HTML5 notification API. See http://notifications.spec.whatwg.org/ for more details.
 * @author Heiko Braun
 * @date 3/27/12
 */
public interface MessageCenter {
    void notify(Message message);
    void addMessageListener(MessageListener listener);
    List<Message> getMessages();
    int getNewMessageCount();
}
