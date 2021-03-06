import * as React from "react";
import BankingEditView from "./BankingEditView";
import BankingCreateView from "./BankingCreateView";
import { openInternalLink } from "../../../../common/utils/links";
import { PaymentInType } from "../consts";

class BankingEditViewResolver extends React.PureComponent<any, any> {
  openNestedView = item => {
    const url = `/${item.paymentTypeName === PaymentInType ? "paymentIn" : "paymentOut"}/` + item.paymentId;
    openInternalLink(url);
  };

  render() {
    const { isNew } = this.props;
    return isNew ? (
      <BankingCreateView {...this.props} openNestedView={this.openNestedView} />
    ) : (
      <BankingEditView {...this.props} openNestedView={this.openNestedView} />
    );
  }
}

export default BankingEditViewResolver;
