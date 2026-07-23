package com.mobilestore.mobile_store.service;

import com.mobilestore.mobile_store.entity.Order;

public interface PdfService {
    byte[] generateInvoicePdf(Order order);
}
