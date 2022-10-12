### Bank Account Insights

Given a request of the following form

```json
{
  "sortCode": "123456",
  "accountNumber": "12345678"
}
```

the API may provide a response of the following form

```json
{
    "bankAccountInsightsCorrelationId": "ab8514f3-0f3c-4823-aba6-58f2222c33f1",
    "riskScore": 100,
    "reason": "ACCOUNT_ON_WATCH_LIST"
}
```

* `bankAccountInsightsCorrelationId` - A unique `UUID` to allow the request/response to be tracked
* `riskScore`     - A score indicating the _riskiness_ of the bank account in question. `0` indicates low risk and `100` indicate high risk
* `reason`        - The reason for the score. `ACCOUNT_ON_WATCH_LIST` indicates the bank account in question was found on a watch list, `ACCOUNT_NOT_ON_WATCH_LIST` indicates it was not found on any watch list


#### Response status codes
* **200** - The request was serviced
* **400** - The request payload was not valid


### Bank Account Verification

This API enables your application to verify that a personal or business bank account exists.
Given an account number, sortcode and account name the response will indicate whether:

* The sortcode exists in the EISCD database of known sortcodes.
* The accountNumber/sortCode combination is valid according to static checks (the modcheck).
* If the above succeed, whether the account name provided matches the name held for the given account.
