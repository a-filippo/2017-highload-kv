package ru.mail.polis;

public class HttpHelpers {
    public static final int STATUS_SUCCESS = 200;
    public static final int STATUS_SUCCESS_GET = 200;
    public static final int STATUS_SUCCESS_PUT = 201;
    public static final int STATUS_SUCCESS_DELETE = 202;
    public static final int STATUS_NOT_FOUND = 404;
    public static final int STATUS_NOT_ENOUGH_REPLICAS = 504;
    public static final int STATUS_BAD_ARGUMENT = 400;
    public static final int STATUS_INTERNAL_ERROR = 500;

    public static final String HEADER_FROM_REPLICAS = "From-Storage";
    public static final String HEADER_HASH_OF_VALUE = "Hash-Of-Value";
}