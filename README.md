# Bank Account Gateway

The `bank-account-gatway` service is a `protected` zone microservice used as the entry point to CIP bank account attribute insights services for external consumers via the API Platform. It is available in `development`, `qa`, `staging`, `externaltest` and `production`

This repository also contains the documentation for all bank account endpoints provided by CIP (Customer Insight Platform) relating to the bank-account `attribute` 

**NB**: Any requests to this microservice that originate outside of the API platform will be rejected. Internal HMRC consumers (both MDTP and corporate tier) should connect to the downstream microservices listed next to each endpoint. 

## API Platform

This is a `private` API, and as such you will need to be subscribed to it manually by our team before you will be able to connect or browse the documentation in the developer hub. Please [contact us](#contact) for more information.


## Endpoints

Please follow the links below for endpoint-specific documentation.

**NB**: The gateway is a proxy microservice and does not implement any business logic. Please refer to the `microservice-name` after each endpoint for implementation details.  

* [POST /verify/business](public/api/conf/1.0/docs/business/assess.md) - `bank-account-reputation`
* [POST /verify/personal](public/api/conf/1.0/docs/personal/assess.md) - `bank-account-reputation`
* [POST /check/insights](public/api/conf/1.0/docs/insights/insights.md) - `bank-account-insights`

## Contact

Our preferred contact method is our public channel in HMRC Digital Slack: `#team-cip-attribute-reputation`

If you do not have access to Slack, please email us at `cip-attribute-reputation-g@digital.hmrc.gov.uk`

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

