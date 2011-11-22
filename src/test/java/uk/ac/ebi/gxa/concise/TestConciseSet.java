package uk.ac.ebi.gxa.concise;

import it.uniroma3.mat.extendedset.intset.ConciseSet;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class TestConciseSet {
    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        ConciseSet cs = new ConciseSet();

        cs.add(1);
        cs.add(2);
        cs.add(3);
        cs.add(999);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(cs);
        ConciseSet cs1 = (ConciseSet) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();

        assertEquals(cs, cs1);
    }

    @Test
    public void testEmptySerialization() throws IOException, ClassNotFoundException {
        ConciseSet cs = new ConciseSet();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(cs);
        ConciseSet cs1 = (ConciseSet) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();

        assertEquals(cs, cs1);
    }
}
