package com.ctrip.hermes.storage;

import java.util.List;

import com.ctrip.hermes.storage.message.Ack;
import com.ctrip.hermes.storage.message.Message;
import com.ctrip.hermes.storage.range.OffsetRecord;
import com.ctrip.hermes.storage.storage.Offset;
import com.ctrip.hermes.storage.storage.StorageException;

public interface MessageQueue {

    List<Message> read(int batchSize) throws StorageException;

    void write(List<Message> msgs) throws StorageException;

    void ack(List<OffsetRecord> records, Ack ack) throws StorageException;
    
    void seek(Offset offset);
}
