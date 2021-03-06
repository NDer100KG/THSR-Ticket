import io, cv2, sys
from PIL import Image
from requests.models import Response
import matplotlib.pyplot as plt  # type: ignore
import numpy as np  # type: ignore

from thsr_ticket.remote.http_request import HTTPRequest
from thsr_ticket.model.web.booking_form.booking_form import BookingForm
from thsr_ticket.model.web.booking_form.ticket_num import AdultTicket
from thsr_ticket.model.web.confirm_train import ConfirmTrain
from thsr_ticket.model.web.confirm_ticket import ConfirmTicket
from thsr_ticket.view_model.avail_trains import AvailTrains
from thsr_ticket.view_model.error_feedback import ErrorFeedback
from thsr_ticket.view_model.booking_result import BookingResult
from thsr_ticket.view.web.booking_form_info import BookingFormInfo
from thsr_ticket.view.web.show_avail_trains import ShowAvailTrains
from thsr_ticket.view.web.show_error_msg import ShowErrorMsg
from thsr_ticket.view.web.confirm_ticket_info import ConfirmTicketInfo
from thsr_ticket.view.web.show_booking_result import ShowBookingResult
from thsr_ticket.view.common import history_info
from thsr_ticket.model.db import ParamDB, Record
from thsr_ticket.nn.model import CNN
import torch


class BookingFlow:
    def __init__(self) -> None:
        self.client = HTTPRequest()

        self.book_form = BookingForm()
        self.book_info = BookingFormInfo()

        self.confirm_train = ConfirmTrain()
        self.show_avail_trains = ShowAvailTrains()

        self.confirm_ticket = ConfirmTicket()
        self.confirm_ticket_info = ConfirmTicketInfo()

        self.error_feedback = ErrorFeedback()
        self.show_error_msg = ShowErrorMsg()

        self.db = ParamDB()
        self.record = Record()

        self.use_nn = True
        if self.use_nn:
            self.model = self.setup_model()

    def setup_model(self):
        model = CNN()
        model.load("thsr_ticket/nn/model.pth")
        model.cuda()
        model.eval()
        return model

    def run(self) -> Response:
        self.show_history()

        # First page. Booking options
        self.set_start_station()
        self.set_dest_station()
        self.set_date()
        self.set_search_by()
        if self.book_form.search_by == 0:
            self.set_outbound_time()
        elif self.book_form.search_by == 1:
            self.set_car_id()
        else:
            assert NotImplementedError
        self.set_adult_ticket_num()

        if self.use_nn:
            self.book_form.security_code = self.classify_security_code()
        else:
            self.book_form.security_code = self.input_security_code()

        form_params = self.book_form.get_params()
        result = self.client.submit_booking_form(form_params)
        if self.show_error(result.content):
            return False

        # Second page. Train confirmation
        if self.book_form.search_by == 0:
            avail_trains = AvailTrains().parse(result.content)
            sel = self.show_avail_trains.show(avail_trains)
            # Selection from UI count from 1
            value = avail_trains[sel - 1].form_value
            self.confirm_train.selection = value
            confirm_params = self.confirm_train.get_params()
            result = self.client.submit_train(confirm_params)
            if self.show_error(result.content):
                return False

        # Third page. Ticket confirmation
        self.set_personal_id()
        self.set_phone()
        ticket_params = self.confirm_ticket.get_params()
        result = self.client.submit_ticket(ticket_params)
        if self.show_error(result.content):
            return False

        result_model = BookingResult().parse(result.content)
        book = ShowBookingResult()
        book.show(result_model)
        print("\n請使用官方提供的管道完成後續付款以及取票!!")

        return True

    def show_history(self) -> None:
        hist = self.db.get_history()
        h_idx = history_info(hist)
        if h_idx is not None:
            self.record = hist[h_idx]

    def set_start_station(self) -> None:
        if self.record.start_station is not None:
            self.book_form.start_station = self.record.start_station
        else:
            self.book_form.start_station = self.book_info.station_info("啟程")

    def set_dest_station(self) -> None:
        if self.record.dest_station is not None:
            self.book_form.dest_station = self.record.dest_station
        else:
            self.book_form.dest_station = self.book_form.dest_station = self.book_info.station_info(
                "到達"
            )

    def set_date(self) -> None:
        if self.record.outbound_date is not None:
            self.book_form.outbound_date = self.record.outbound_date
        else:
            self.book_form.outbound_date = self.book_info.date_info("出發")

    def set_search_by(self) -> None:
        if self.record.search_by is not None:
            self.book_form.search_by = self.record.search_by
        else:
            print("0. 依時間搜尋")
            print("1. 直接輸入車次")
            self.book_form.search_by = int(input("輸入選擇(預設: {}): ".format(0)) or 0)

    def set_outbound_time(self) -> None:
        if self.record.outbound_time is not None:
            self.book_form.outbound_time = self.record.outbound_time
        else:
            self.book_form.outbound_time = self.book_info.time_table_info()

    def set_car_id(self) -> None:
        if self.record.car_id is not None:
            self.book_form.car_id = self.record.car_id
        else:
            self.book_form.car_id = int(input("輸入車號(預設: {}): ".format(0)) or 0)

    def set_adult_ticket_num(self) -> None:
        if self.record.adult_num is not None:
            self.book_form.adult_ticket_num = self.record.adult_num
        else:
            sel = self.book_info.ticket_num_info("大人", default_value=1)
            self.book_form.adult_ticket_num = AdultTicket().get_code(sel)

    def set_personal_id(self) -> None:
        if self.record.personal_id is not None:
            self.confirm_ticket.personal_id = self.record.personal_id
        else:
            self.confirm_ticket.personal_id = self.confirm_ticket_info.personal_id_info()

    def set_phone(self) -> None:
        if self.record.phone is not None:
            self.confirm_ticket.phone = self.record.phone
        else:
            self.confirm_ticket.phone = self.confirm_ticket_info.phone_info()

    def input_security_code(self) -> str:
        print("等待驗證碼...")
        book_page = self.client.request_booking_page()
        img_resp = self.client.request_security_code_img(book_page.content)
        image = Image.open(io.BytesIO(img_resp.content))
        print("輸入驗證碼:")
        img_arr = np.array(image)
        plt.imshow(img_arr)
        plt.show(block=False)
        return input()
    
    def classify_security_code(self) -> str:
        book_page = self.client.request_booking_page()
        img_resp = self.client.request_security_code_img(book_page.content)
        image_ori = np.array(Image.open(io.BytesIO(img_resp.content)))
        image = cv2.resize(image_ori, (128, 128))
        with torch.no_grad():
            image = (
                cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)[np.newaxis, np.newaxis, :, :] / 255.0
            )
            image = torch.tensor(image, dtype=torch.float32).cuda()
            pred = CNN.decode(self.model(image))
            pred = "".join(pred)

        ### show debug image
        # cv2.putText(image_ori, pred, (20, 20), 3, 1, (255, 0, 255))
        # cv2.imshow("img", image_ori)
        # cv2.waitKey(0)

        return pred

    def show_error(self, html: bytes) -> bool:
        errors = self.error_feedback.parse(html)
        if len(errors) == 0:
            return False

        self.show_error_msg.show(errors)
        return True
