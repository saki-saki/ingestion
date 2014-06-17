package com.stratio.ingestion.deserializer.compression;

import com.google.common.base.Charsets;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.conf.ConfigurationException;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.serialization.EventDeserializer;
import org.apache.flume.serialization.EventDeserializerFactory;
import org.apache.flume.serialization.ResettableInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(JUnit4.class)
public class DecompressDeserializerTest {

    private static final Logger log = LoggerFactory.getLogger(DecompressDeserializerTest.class);

    private ResettableInputStream createResettableInputStream(final String text) throws IOException, CompressorException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final OutputStream outputStream = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.GZIP, byteArrayOutputStream);
        IOUtils.write(text, outputStream);
        outputStream.flush();
        outputStream.close();
        return new ResettableByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    @Test
    public void basic() throws IOException, CompressorException {
        ResettableInputStream resettableInputStream = createResettableInputStream("ONE\nTWO\nTHREE\n");

        Context context = new Context();
        context.put("format", "gzip");
        context.put("trackerFile", "/tmp/.tracker_file");

        EventDeserializer eventDeserializer = EventDeserializerFactory.getInstance(
                "com.stratio.ingestion.deserializer.compression.DecompressDeserializer$Builder", context, resettableInputStream);

        List<Event> events = eventDeserializer.readEvents(10);
        eventDeserializer.close();

        List<Event> expectedEvents = Arrays.asList(
                EventBuilder.withBody("ONE", Charsets.UTF_8),
                EventBuilder.withBody("TWO", Charsets.UTF_8),
                EventBuilder.withBody("THREE", Charsets.UTF_8)
        );
        assertThat(events.size()).isEqualTo(expectedEvents.size());
        for (int i = 0; i < events.size(); i++) {
            assertThat(events.get(i).getBody()).isEqualTo(expectedEvents.get(i).getBody());
        }

    }

    @Test
    public void fullReset() throws IOException, CompressorException {
        ResettableInputStream resettableInputStream = createResettableInputStream("ONE\nTWO\nTHREE\n");

        File trackerFile = File.createTempFile(".tracker_file", ".meta");
        log.debug("fullReset() - trackerFile = {}", trackerFile.getAbsolutePath());
        trackerFile.delete();

        Context context = new Context();
        context.put("format", "gzip");
        context.put("trackerFile", trackerFile.getAbsolutePath());

        EventDeserializer eventDeserializer = EventDeserializerFactory.getInstance(
                "com.stratio.ingestion.deserializer.compression.DecompressDeserializer$Builder", context, resettableInputStream);

        List<Event> events = eventDeserializer.readEvents(1);
        eventDeserializer.mark();
        eventDeserializer.close();
        resettableInputStream = createResettableInputStream("ONE\nTWO\nTHREE\n");
        eventDeserializer = EventDeserializerFactory.getInstance(
                "com.stratio.ingestion.deserializer.compression.DecompressDeserializer$Builder", context, resettableInputStream);
        events.addAll(eventDeserializer.readEvents(10));
        eventDeserializer.close();

        List<Event> expectedEvents = Arrays.asList(
                EventBuilder.withBody("ONE", Charsets.UTF_8),
                EventBuilder.withBody("TWO", Charsets.UTF_8),
                EventBuilder.withBody("THREE", Charsets.UTF_8)
        );
        assertThat(events.size()).isEqualTo(expectedEvents.size());
        for (int i = 0; i < events.size(); i++) {
            assertThat(events.get(i).getBody()).isEqualTo(expectedEvents.get(i).getBody());
        }

    }

    @Test
    public void eventDeserializerFactoryInstantiation() throws IOException, CompressorException {
        Context context = new Context();
        context.put("format", "gzip");
        context.put("trackerFile", "/tmp/.tracker_file");
        ResettableInputStream resettableInputStream = createResettableInputStream("ONE\nTWO\nTHREE\n");
        EventDeserializer eventDeserializer = EventDeserializerFactory.getInstance(
                "com.stratio.ingestion.deserializer.compression.DecompressDeserializer$Builder", context, resettableInputStream);
        assertThat(eventDeserializer).isInstanceOf(DecompressDeserializer.class);
    }

    @Test(expected = ConfigurationException.class)
    public void eventDeserializerFactoryInstantiationWithoutFormat() throws IOException, CompressorException {
        Context context = new Context();
        context.put("trackerFile", "/tmp/.tracker_file");
        ResettableInputStream resettableInputStream = createResettableInputStream("ONE\nTWO\nTHREE\n");
        EventDeserializer eventDeserializer = EventDeserializerFactory.getInstance(
                "com.stratio.ingestion.deserializer.compression.DecompressDeserializer$Builder", context, resettableInputStream);
    }

    @Test(expected = ConfigurationException.class)
    public void eventDeserializerFactoryInstantiationWithoutTrackerDir() throws IOException, CompressorException {
        Context context = new Context();
        context.put("format", "gzip");
        ResettableInputStream resettableInputStream = createResettableInputStream("ONE\nTWO\nTHREE\n");
        EventDeserializer eventDeserializer = EventDeserializerFactory.getInstance(
                "com.stratio.ingestion.deserializer.compression.DecompressDeserializer$Builder", context, resettableInputStream);
    }

}
