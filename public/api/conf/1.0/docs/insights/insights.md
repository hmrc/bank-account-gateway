# Overview

### Bank Account Insights

This API enables your application to get an opinion of the riskiness of a sortcode and bank account combination.

Given a sort code and account number, the response will provide:

* Risk score from 0 (no risk) to 100 (high risk)
* Reason providing an indication of why the risk score has been allocated
* Correlation Id - so you can reference the transaction in any feedback

### What and how?
This endpoint checks bank account details against a watch list of accounts known or suspected to have been involved in fraudulent activity. If the details are present on the list a score of 100 is returned. Otherwise, we return 0.

This logic will change and become more refined as more checks are added.

### How to use the results
The intended consumers of this API are services preparing to make payments to a bank account using details provided by a user. 

You may choose to reject any payments with a score above a certain threshold, or log this result in your own systems pending investigation.

### Request\response details

All requests must include a uniquely identifiable `user-agent` header. Please contact us for assistance when first connecting.  

* POST /check/insights
    * [Request](example-check-post-request.json) ([schema](check-post-request.json))
    * [Response](example-check-response.json) ([schema](check-response.json))

