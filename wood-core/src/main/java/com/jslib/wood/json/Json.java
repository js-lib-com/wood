package com.jslib.wood.json;

import com.jslib.wood.lang.Event;

public interface Json {
    static Json getInstance() {
        return JsonImpl.getInstance();
    }

    String stringify(Object object);
}
