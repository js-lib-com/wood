package com.jslib.wood.json;

public interface Json {
    static Json getInstance() {
        return JsonImpl.getInstance();
    }

    String stringify(Object object);
}
