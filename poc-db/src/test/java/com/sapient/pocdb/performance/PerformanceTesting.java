package com.sapient.pocdb.performance;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerformanceTesting {

    /* INFO: ArrayLists benefit greatly from parallelisation for computation. */

    @RepeatedTest(5)
    void multiplyArrayListParallel() {
        int size = 10000;
        ArrayList<Integer> inputList = new ArrayList();
        IntStream.rangeClosed(1, size).boxed().forEach(inputList::add);

        List<Integer> resultList = inputList.stream().parallel().map(input -> input * 2).collect(Collectors.toList());
        assertEquals(size, resultList.size());
    }

    @RepeatedTest(5)
    void multiplyArrayListSequential() {
        int size = 100000;
        ArrayList<Integer> inputList = new ArrayList();
        IntStream.rangeClosed(1, size).boxed().forEach(inputList::add);

        List<Integer> resultList = inputList.stream().map(input -> input * 2).collect(Collectors.toList());
        assertEquals(size, resultList.size());
    }

    /* INFO: LinkedLists do not parallelise well - it will often be quicker to run streams on a LinkedList in sequential mode. */

    @RepeatedTest(5)
    void multiplyLinkedListParallel() {
        int size = 100000;
        LinkedList<Integer> inputList = new LinkedList<>();
        IntStream.rangeClosed(1, size)
                .boxed()
                .forEach(inputList::add);

        List<Integer> resultList = inputList.stream().parallel().map(integer -> integer * 2).collect(Collectors.toList());

        assertEquals(size, resultList.size());
    }

    @RepeatedTest(5)
    void multiplyLinkedListSequential() {
        int size = 100000;
        LinkedList<Integer> inputList = new LinkedList<>();
        IntStream.rangeClosed(1, size)
                .boxed()
                .forEach(inputList::add);

        List<Integer> resultList = inputList.stream().map(integer -> integer * 2).collect(Collectors.toList());

        assertEquals(size, resultList.size());
    }


    /* INFO: Be careful when using streams with sets - if ordering matters then using a set may not be the answer. */

    @Test
    void multiplySetSequential() {
        Set<Integer> integerSet = new TreeSet<>();
        for(int i = 1; i < 9; ++i) {
            integerSet.add(i);
        }
        Set<Integer> finalSet = integerSet.stream().map(integer -> integer * 2).collect(Collectors.toSet());
        System.out.println(finalSet);
        assertEquals(8, finalSet.size());
    }

    @Test
    void multiplySetParallel() {
        Set<Integer> integerSet = new TreeSet<>();
        for(int i = 1; i < 9; ++i) {
            integerSet.add(i);
        }
        integerSet = integerSet.stream().parallel().map(integer -> integer * 2).collect(Collectors.toSet());
        System.out.println(integerSet);
        assertEquals(8, integerSet.size());
    }
}


