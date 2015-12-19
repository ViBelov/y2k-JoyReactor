package y2k.joyreactor.services.synchronizers;

import rx.Observable;
import y2k.joyreactor.Message;
import y2k.joyreactor.common.ObservableUtils;
import y2k.joyreactor.services.repository.MessageForDateQuery;
import y2k.joyreactor.services.repository.Repository;
import y2k.joyreactor.services.requests.MessageListRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by y2k on 11/17/15.
 */
public class PrivateMessageFetcher {

    private MessageListRequest request;
    private Repository<Message> repository;

    private Date mineOldest;
    private Date theirOldest;

    public PrivateMessageFetcher() {
        this(new MessageListRequest(), new Repository<>(Message.class));
    }

    public PrivateMessageFetcher(MessageListRequest request, Repository<Message> repository) {
        this.request = request;
        this.repository = repository;
    }

    public Observable<Void> execute() {
        return ObservableUtils.action(() -> {
            while (true) {
                request.execute(request.getNextPage());

                updateLastMessageDates();
                boolean needLoadNext = isNeedLoadNext();

                new NewMessageSaver().save(request.getMessages());

                if (request.getNextPage() == null)
                    break;
                if (!needLoadNext)
                    break;
            }
        });
    }

    private void updateLastMessageDates() {
        mineOldest = theirOldest = null;
        for (Message m : request.getMessages()) {
            if (m.isMine) mineOldest = m.date;
            else theirOldest = m.date;
        }
    }

    private boolean isNeedLoadNext() {
        if (mineOldest != null) {
            if (repository.queryFirst(new MessageForDateQuery(mineOldest, true)) == null)
                return true;
        }
        if (theirOldest != null) {
            if (repository.queryFirst(new MessageForDateQuery(theirOldest, false)) == null)
                return true;
        }
        return false;
    }

    class NewMessageSaver {

        public void save(List<Message> messages) {
            List<Message> newMessages = new ArrayList<>();
            for (Message m : messages)
                if (isNotInRepository(m))
                    newMessages.add(m);

            if (!newMessages.isEmpty())
                repository.insertAll(newMessages);
        }

        private boolean isNotInRepository(Message m) {
            return repository.queryFirst(new MessageForDateQuery(m.date, m.isMine)) == null;
        }
    }
}