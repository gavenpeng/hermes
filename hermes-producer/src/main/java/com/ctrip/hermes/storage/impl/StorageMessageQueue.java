package com.ctrip.hermes.storage.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ctrip.hermes.storage.MessageQueue;
import com.ctrip.hermes.storage.message.Message;
import com.ctrip.hermes.storage.message.Resend;
import com.ctrip.hermes.storage.pair.StoragePair;
import com.ctrip.hermes.storage.range.OffsetRecord;
import com.ctrip.hermes.storage.range.RangeEvent;
import com.ctrip.hermes.storage.range.RangeStatusListener;
import com.ctrip.hermes.storage.storage.Offset;
import com.ctrip.hermes.storage.storage.Range;
import com.ctrip.hermes.storage.storage.StorageException;

public class StorageMessageQueue implements MessageQueue {

	private StoragePair<Message> m_msgPair;

	private StoragePair<Resend> m_resendPair;

	private Map<String, StoragePair<?>> m_id2Pair = new HashMap<String, StoragePair<?>>();

	public StorageMessageQueue(StoragePair<Message> main, StoragePair<Resend> resend) {
		m_msgPair = main;
		m_resendPair = resend;

		recordPair(main.getStorageIds(), main);
		recordPair(resend.getStorageIds(), resend);

		connectPairs(main, resend);
	}

	private void recordPair(List<String> storageIds, StoragePair<?> pair) {
		for (String id : storageIds) {
			m_id2Pair.put(id, pair);
		}
	}

	@Override
	public List<Message> read(int batchSize) throws StorageException {
		int remain = batchSize;

		List<Message> result = m_msgPair.readMain(batchSize);
		m_msgPair.waitForAck(result);
		remain -= result.size();

		if (remain > 0) {
			List<Resend> resends = m_resendPair.readMain(remain);

			for (Resend resend : resends) {
				if (remain <= 0) {
					break;
				}

				Range r = resend.getRange();
				List<Message> resendMsgs = m_msgPair.readMain(r);

				for (Message msg : resendMsgs) {
					msg.setAckOffset(resend.getOffset());
				}

				m_resendPair.waitForAck(resendMsgs, resend.getOffset());

				result.addAll(resendMsgs);
				// TODO maybe more than batch size
				remain -= resendMsgs.size();
			}
		}

		return result;
	}

	@Override
	public void write(List<Message> msgs) throws StorageException {
		m_msgPair.appendMain(msgs);
	}

	// TODO consumer with same groupId should connect to the same message queue
	// to mediate message dispatch
	@Override
	public void ack(List<OffsetRecord> records) throws StorageException {
		for (OffsetRecord rec : records) {
			StoragePair<?> pair = m_id2Pair.get(rec.getToUpdate().getId());
			if (pair == null) {
				throw new RuntimeException(rec.getToUpdate().getId() + " not found in " + m_id2Pair);
			}
			pair.ack(rec);
		}
	}

	@Override
	public void seek(Offset offset) {
		// TODO Auto-generated method stub

	}

	private void connectPairs(StoragePair<Message> mainPair, final StoragePair<Resend> resendPair) {
		mainPair.addRangeStatusListener(new RangeStatusListener() {

			@Override
			public void onRangeSuccess(RangeEvent event) throws StorageException {
			}

			@Override
			public void onRangeFail(RangeEvent event) throws StorageException {
				// TODO
				resendPair.appendMain(new Resend(event.getRecord().getToBeDone(), 100));
			}
		});

		resendPair.addRangeStatusListener(new RangeStatusListener() {

			@Override
			public void onRangeSuccess(RangeEvent event) throws StorageException {
			}

			@Override
			public void onRangeFail(RangeEvent event) throws StorageException {
				// TODO dead letter
				resendPair.appendMain(new Resend(event.getRecord().getToBeDone(), 100));
			}
		});
	}

}