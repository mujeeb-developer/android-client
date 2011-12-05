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

package org.mifos.androidclient.main;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TabHost;
import org.mifos.androidclient.R;
import org.mifos.androidclient.entities.account.AbstractAccountDetails;
import org.mifos.androidclient.entities.account.SavingsAccountDetails;
import org.mifos.androidclient.entities.customer.AccountBasicInformation;
import org.mifos.androidclient.net.services.AccountService;
import org.mifos.androidclient.templates.AccountDetailsViewBuilder;
import org.mifos.androidclient.templates.DownloaderActivity;
import org.mifos.androidclient.templates.ServiceConnectivityTask;
import org.mifos.androidclient.templates.ViewBuilderFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

public class AccountDetailsActivity extends DownloaderActivity {

    public static final String SELECTED_TAB_BUNDLE_KEY = AccountDetailsActivity.class.getSimpleName() + "-selectedTab";

    private AccountBasicInformation mAccount;
    private AbstractAccountDetails mDetails;
    private AccountService mAccountService;
    private AccountDetailsTask mAccountDetailsTask;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.account_details);

        TabHost tabs = (TabHost)findViewById(R.id.accountDetails_tabHost);
        tabs.setup();
        TabHost.TabSpec overviewSpec = tabs.newTabSpec(getString(R.string.accountDetails_tab_overview));
        overviewSpec.setIndicator(getString(R.string.accountDetails_tab_overview));
        overviewSpec.setContent(R.id.account_overview);
        TabHost.TabSpec additionalInfoSpec = tabs.newTabSpec(getString(R.string.accountDetails_tab_additionalInfo));
        additionalInfoSpec.setIndicator(getString(R.string.accountDetails_tab_additionalInfo));
        additionalInfoSpec.setContent(R.id.account_details);
        tabs.addTab(overviewSpec);
        tabs.addTab(additionalInfoSpec);

        if (bundle != null) {
            if (bundle.containsKey(AbstractAccountDetails.BUNDLE_KEY)) {
                mDetails = (AbstractAccountDetails)bundle.getSerializable(AbstractAccountDetails.BUNDLE_KEY);
            }
            if (bundle.containsKey(SELECTED_TAB_BUNDLE_KEY)) {
                tabs.setCurrentTab(bundle.getInt(SELECTED_TAB_BUNDLE_KEY));
            }
        }

        mAccount = (AccountBasicInformation)getIntent().getSerializableExtra(AccountBasicInformation.BUNDLE_KEY);
        mAccountService = new AccountService(this);
    }

    @Override
    protected void onSessionActive() {
        super.onSessionActive();
        if (mDetails == null) {
            runAccountDetailsTask();
        } else {
            updateContent(mDetails);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAccountDetailsTask != null) {
            mAccountDetailsTask.terminate();
            mAccountDetailsTask = null;
        }
    }

    /**
     * A handler of the button for account's transactions history browsing.
     *
     * @param view the view on in which the pressed button resides
     */
    public void onTransactionsHistorySelected(View view) {
        Intent intent = new Intent().setClass(this, AccountTransactionHistoryActivity.class);
        intent.putExtra(AbstractAccountDetails.ACCOUNT_NUMBER_BUNDLE_KEY, mAccount.getGlobalAccountNum());
        startActivity(intent);
    }

    /**
     * A handler of the button for savings account's deposit due details browsing.
     *
     * @param view the view on which the button resides
     */
    public void onDepositDueDetailsSelected(View view) {

    }

    /**
     * A handler of the button for loan account's installment details browsing.
     *
     * @param view the view on in which the pressed button resides
     */
    public void onInstallmentDetailsSelected(View view) {
        Intent intent = new Intent().setClass(this, LoanInstallmentDetailsActivity.class);
        intent.putExtra(AbstractAccountDetails.ACCOUNT_NUMBER_BUNDLE_KEY, mAccount.getGlobalAccountNum());
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(AbstractAccountDetails.BUNDLE_KEY, mDetails);
        TabHost tabs = (TabHost)findViewById(R.id.accountDetails_tabHost);
        if (tabs != null) {
            outState.putSerializable(SELECTED_TAB_BUNDLE_KEY, tabs.getCurrentTab());
        }
    }

    private void updateContent(AbstractAccountDetails details) {
        if (details != null) {
            mDetails = details;
            ViewBuilderFactory factory = new ViewBuilderFactory(this);
            AccountDetailsViewBuilder builder = factory.createAccountDetailsViewBuilder(details);

            LinearLayout tabContent = (LinearLayout)findViewById(R.id.account_overview);
            if (tabContent.getChildCount() > 0) {
                tabContent.removeAllViews();
            }
            tabContent.addView(builder.buildOverviewView());

            tabContent = (LinearLayout)findViewById(R.id.account_details);
            if (tabContent.getChildCount() > 0) {
                tabContent.removeAllViews();
            }
            tabContent.addView(builder.buildDetailsView());

            if (details.getClass() == SavingsAccountDetails.class) {
                if (((SavingsAccountDetails)details).getDepositTypeName().equals(SavingsAccountDetails.MANDATORY_DEPOSIT)) {
                    Button depositDueButton = (Button)findViewById(R.id.view_depositDueDetails_button);
                    depositDueButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void runAccountDetailsTask() {
        if (mAccount == null || !StringUtils.hasLength(mAccount.getGlobalAccountNum())) {
            mUIUtils.displayLongMessage(getString(R.string.toast_customer_id_not_available));
            return;
        }
        if (mAccountDetailsTask == null || mAccountDetailsTask.getStatus() != AsyncTask.Status.RUNNING) {
            mAccountDetailsTask = new AccountDetailsTask(
                    this,
                    getString(R.string.dialog_getting_account_data),
                    getString(R.string.dialog_loading_message)
            );
            mAccountDetailsTask.execute(mAccount);
        }
    }

    private class AccountDetailsTask extends ServiceConnectivityTask<AccountBasicInformation, Void, AbstractAccountDetails> {

        public AccountDetailsTask(Context context, String progressTitle, String progressMessage) {
            super(context, progressTitle, progressMessage);
        }

        @Override
        protected AbstractAccountDetails doInBackgroundBody(AccountBasicInformation... params) throws RestClientException, IllegalArgumentException {
            AbstractAccountDetails details = null;
            if (mAccountService != null) {
                details = mAccountService.getAccountDetailsForEntity(params[0]);
            }
            return details;
        }

        @Override
        protected void onPostExecuteBody(AbstractAccountDetails details) {
            if (details != null) {
                updateContent(details);
            }
        }
    }

}
