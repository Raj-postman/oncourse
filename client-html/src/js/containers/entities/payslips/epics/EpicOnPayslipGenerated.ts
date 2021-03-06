/*
 * Copyright ish group pty ltd. All rights reserved. https://www.ish.com.au
 * No copying or use of this code is allowed without permission in writing from ish.
 */

import { ActionsObservable, Epic } from "redux-observable";
import { mergeMap } from "rxjs/operators";
import { State } from "../../../../reducers/state";
import { PAYROLL_PROCESS_FINISHED } from "../../payrolls/actions";
import { openInternalLink } from "../../../../common/utils/links";

export const EpicOnPayslipGenerated: Epic<any, State> = (action$: ActionsObservable<any>): any => action$.ofType(PAYROLL_PROCESS_FINISHED).pipe(
    mergeMap(action => {
      const { maxPayslipId } = action.payload;

      if (maxPayslipId) {
        openInternalLink(`/payslip/?search=id > ${maxPayslipId} `);
      }
      return [];
    })
  );
