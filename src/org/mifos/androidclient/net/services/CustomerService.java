/*
 * Copyright (c) 2005-2011 Grameen Foundation USA
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 * explanation of the license and how it is applied.
 */

package org.mifos.androidclient.net.services;

import android.content.Context;
import org.mifos.androidclient.entities.customer.*;
import org.mifos.androidclient.entities.simple.*;

public class CustomerService extends RestNetworkService {

    private final static String LOAN_OFFICER_CUSTOMERS_PATH = "/personnel/clients/id-current.json";

    private final static String CLIENT_DETAILS_PATH_PREFIX = "/client/num-";
    private final static String GROUP_DETAILS_PATH_PREFIX = "/group/num-";
    private final static String CENTER_DETAILS_PATH_PREFIX = "/center/num-";

    private final static String CLIENT_CHARGES_DETAILS_PATH_PREFIX = "/client/charges/num-";

    public CustomerService(Context context) {
        super(context);
    }

    public CustomersData getLoanOfficersCustomers() {
        String url = getServerUrl() + LOAN_OFFICER_CUSTOMERS_PATH;
        return mRestConnector.getForObject(url, CustomersData.class);
    }

    public ClientDetails getClientDetails(String customerNumber) {
        String url = getServerUrl() + CLIENT_DETAILS_PATH_PREFIX + customerNumber + PATH_SUFFIX;
        return mRestConnector.getForObject(url, ClientDetails.class);
    }

    public GroupDetails getGroupDetails(String groupNumber) {
        String url = getServerUrl() + GROUP_DETAILS_PATH_PREFIX + groupNumber + PATH_SUFFIX;
        return mRestConnector.getForObject(url, GroupDetails.class);
    }

    public CenterDetails getCenterDetails(String centerNumber) {
        String url = getServerUrl() + CENTER_DETAILS_PATH_PREFIX + centerNumber + PATH_SUFFIX;
        return mRestConnector.getForObject(url, CenterDetails.class);
    }

    public CustomerDetailsEntity getDetailsForEntity(AbstractCustomer customer) {
        CustomerDetailsEntity details = null;
        if (customer.getClass() == Customer.class) {
            details = getClientDetails(customer.getGlobalCustNum());
        } else if (customer.getClass() == Group.class) {
            details = getGroupDetails(customer.getGlobalCustNum());
        } else if (customer.getClass() == Center.class) {
            details = getCenterDetails(customer.getGlobalCustNum());
        }
        return details;
    }

    public CustomerChargesDetails getChargesForEntity(AbstractCustomer entity) {
        CustomerChargesDetails details = null;
        String url;
        if (entity.getClass() == Customer.class) {
            url = getServerUrl() + CLIENT_CHARGES_DETAILS_PATH_PREFIX + entity.getGlobalCustNum() + PATH_SUFFIX;
            details = mRestConnector.getForObject(url, CustomerChargesDetails.class);
        }
        return details;
    }

}
