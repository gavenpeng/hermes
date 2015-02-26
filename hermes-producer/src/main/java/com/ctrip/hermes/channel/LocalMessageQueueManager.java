package com.ctrip.hermes.channel;

import java.util.HashMap;
import java.util.Map;

import org.unidal.lookup.annotation.Inject;
import org.unidal.tuple.Pair;

import com.ctrip.hermes.meta.MetaService;
import com.ctrip.hermes.meta.entity.Storage;
import com.ctrip.hermes.storage.MessageQueue;
import com.ctrip.hermes.storage.impl.StorageMessageQueue;
import com.ctrip.hermes.storage.message.Message;
import com.ctrip.hermes.storage.message.Resend;
import com.ctrip.hermes.storage.pair.StoragePair;
import com.ctrip.hermes.storage.storage.memory.MemoryGroup;
import com.ctrip.hermes.storage.storage.memory.MemoryGroupConfig;
import com.ctrip.hermes.storage.storage.memory.MemoryStorageFactory;

public class LocalMessageQueueManager implements MessageQueueManager {

	@Inject
	private MetaService m_meta;

	private Map<Pair<String, String>, StorageMessageQueue> m_queues = new HashMap<Pair<String, String>, StorageMessageQueue>();

	private MemoryStorageFactory storageFactory = new MemoryStorageFactory();

	@Override
	public MessageQueue findQueue(String topic, String groupId) {
		if (Storage.MEMORY.equals(m_meta.getStorageType(topic))) {
			return findMemoryQueue(topic, groupId);
		} else {
			// TODO
			throw new RuntimeException("Unsupported storage type");
		}
	}

	@Override
	public MessageQueue findQueue(String topic) {
		return findMemoryQueue(topic, "invalid");
	}

	private synchronized MessageQueue findMemoryQueue(String topic, String groupId) {
		Pair<String, String> pair = new Pair<String, String>(topic, groupId);

		StorageMessageQueue q = m_queues.get(pair);

		if (q == null) {
			MemoryGroupConfig gc = new MemoryGroupConfig();
			gc.addMainGroup(topic, "offset_" + topic + "_" + groupId);
			gc.setResendGroupId("resend_" + topic + "_" + groupId, "offset_resend_" + topic + "_" + groupId);
			MemoryGroup mg = new MemoryGroup(storageFactory, gc);

			StoragePair<Message> main = mg.createMessagePair();
			StoragePair<Resend> resend = mg.createResendPair();
			q = new StorageMessageQueue(main, resend);

			m_queues.put(pair, q);
		}

		return q;

	}

}