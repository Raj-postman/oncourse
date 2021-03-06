/*
 * Copyright ish group pty ltd. All rights reserved. https://www.ish.com.au
 * No copying or use of this code is allowed without permission in writing from ish.
 */

import { Dispatch } from "redux";
import { SHOW_MESSAGE } from "../../actions/index";
import { ServerResponse } from "../../../model/common/apiHandlers";

const instantFetchErrorHandler = (
  dispatch: Dispatch,
  response: ServerResponse,
  customMessage: string = "Something went wrong. Changes won't be saved"
) => {
  if (!response) {
    dispatch({
      type: SHOW_MESSAGE,
      payload: { message: customMessage || "Something went wrong" }
    });
    return;
  }

  const { data, status } = response;

  if (status) {
    dispatch({
      type: SHOW_MESSAGE,
      payload: {
        message: (data && data.errorMessage) || customMessage
      }
    });
  } else {
    dispatch({
      type: SHOW_MESSAGE,
      payload: {
        message: response
      }
    });
  }
};

export default instantFetchErrorHandler;
