package ncu.cc.proxyz.helpers;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class ListHelper {
    public static <T> Stream<T> listOfListFlattening(List<List<T>> listOfList) {
        return listOfList.stream()
                .flatMap(Collection::stream);
    }

    public static boolean notEmpty(List<?> list) {
        return list != null && list.size() > 0;
    }

}
